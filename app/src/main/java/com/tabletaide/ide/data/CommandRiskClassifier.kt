package com.tabletaide.ide.data

enum class CommandRiskLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
}

data class CommandPreview(
    val command: String,
    val level: CommandRiskLevel,
    val reasons: List<String>,
    val likelyTargets: List<String>,
)

object CommandRiskClassifier {

    fun classify(command: String): CommandPreview {
        val trimmed = command.trim()
        val tokens = tokenize(trimmed)
        val reasons = mutableListOf<String>()
        val targets = linkedSetOf<String>()
        var level = CommandRiskLevel.LOW

        fun bump(to: CommandRiskLevel) {
            if (to.ordinal > level.ordinal) level = to
        }

        if (containsRm(tokens)) {
            bump(CommandRiskLevel.HIGH)
            reasons += "Deletes files (`rm`)"
            targets += rmTargets(tokens)
        }
        if (containsToken(tokens, "mv")) {
            bump(CommandRiskLevel.MEDIUM)
            reasons += "Renames or moves paths (`mv`)"
            targets += operandsAfter(tokens, "mv")
        }
        if (containsToken(tokens, "cp") && hasFlag(tokens, "cp", "-r", "-R", "--recursive")) {
            bump(CommandRiskLevel.MEDIUM)
            reasons += "Recursive copy (`cp -r`)"
        }
        if (containsToken(tokens, "dd")) {
            bump(CommandRiskLevel.HIGH)
            reasons += "Raw block writes (`dd`)"
        }
        if (containsToken(tokens, "chmod") && hasFlag(tokens, "chmod", "-R", "--recursive")) {
            bump(CommandRiskLevel.MEDIUM)
            reasons += "Recursive permission change (`chmod -R`)"
        }
        if (containsToken(tokens, "chown") && hasFlag(tokens, "chown", "-R", "--recursive")) {
            bump(CommandRiskLevel.MEDIUM)
            reasons += "Recursive ownership change (`chown -R`)"
        }
        if (containsGitDestructive(trimmed)) {
            bump(CommandRiskLevel.HIGH)
            reasons += "Destructive git op (reset --hard / clean -fd / checkout -- / branch -D)"
        }
        if (containsRedirect(trimmed)) {
            bump(CommandRiskLevel.MEDIUM)
            reasons += "Shell redirect overwrites a file (`>`)"
            targets += redirectTargets(trimmed)
        }
        if (containsAppendRedirect(trimmed)) {
            bump(CommandRiskLevel.LOW.coerceAtLeast(CommandRiskLevel.MEDIUM))
            reasons += "Shell append redirect (`>>`)"
            targets += appendRedirectTargets(trimmed)
        }
        if (containsToken(tokens, "sudo") || containsToken(tokens, "su")) {
            bump(CommandRiskLevel.HIGH)
            reasons += "Privilege escalation requested"
        }
        if (containsPipeToShell(trimmed)) {
            bump(CommandRiskLevel.HIGH)
            reasons += "Pipes remote content into shell"
        }

        if (reasons.isEmpty()) reasons += "No destructive patterns detected"
        return CommandPreview(
            command = trimmed,
            level = level,
            reasons = reasons,
            likelyTargets = targets.toList(),
        )
    }

    private fun tokenize(command: String): List<String> {
        // Cheap split on whitespace; good enough for classification heuristics.
        // Strips quotes so `rm "foo bar"` surfaces as a single target heuristic
        // but does not need to be perfectly POSIX-correct.
        val raw = command.split(Regex("\\s+")).filter { it.isNotBlank() }
        return raw.map { token -> token.trim('"', '\'') }
    }

    private fun containsToken(tokens: List<String>, name: String): Boolean =
        tokens.any { it == name || it == "/bin/$name" || it == "/usr/bin/$name" }

    private fun containsRm(tokens: List<String>): Boolean {
        if (!containsToken(tokens, "rm") && !containsToken(tokens, "rmdir")) return false
        return true
    }

    private fun rmTargets(tokens: List<String>): List<String> {
        val out = mutableListOf<String>()
        var sawRm = false
        for (t in tokens) {
            if (!sawRm) {
                if (t == "rm" || t == "rmdir" || t.endsWith("/rm") || t.endsWith("/rmdir")) sawRm = true
                continue
            }
            if (t.startsWith("-")) continue
            if (t == ";" || t == "&&" || t == "||" || t == "|" || t == ">" || t == ">>") break
            out += t
        }
        return out
    }

    private fun operandsAfter(tokens: List<String>, head: String): List<String> {
        val idx = tokens.indexOf(head)
        if (idx < 0) return emptyList()
        return tokens.drop(idx + 1).filter { !it.startsWith("-") && it != ";" && it != "&&" && it != "||" }
    }

    private fun hasFlag(tokens: List<String>, head: String, vararg flags: String): Boolean {
        val idx = tokens.indexOf(head)
        if (idx < 0) return false
        return tokens.drop(idx + 1).any { tok -> flags.any { f -> tok == f || (f.length == 2 && tok.startsWith("-") && !tok.startsWith("--") && tok.contains(f[1])) } }
    }

    private fun containsGitDestructive(cmd: String): Boolean {
        val gitPatterns = listOf(
            Regex("\\bgit\\s+reset\\s+--hard\\b"),
            Regex("\\bgit\\s+clean\\s+-[a-zA-Z]*f"),
            Regex("\\bgit\\s+checkout\\s+--\\b"),
            Regex("\\bgit\\s+restore\\b"),
            Regex("\\bgit\\s+branch\\s+-D\\b"),
            Regex("\\bgit\\s+push\\s+.*--force\\b"),
            Regex("\\bgit\\s+push\\s+.*-f\\b"),
        )
        return gitPatterns.any { it.containsMatchIn(cmd) }
    }

    private fun containsRedirect(cmd: String): Boolean {
        // Single `>` not `>>` and not `2>` part of a stderr-only redirect.
        return Regex("(^|[^>0-9])>[^>]").containsMatchIn(cmd)
    }

    private fun containsAppendRedirect(cmd: String): Boolean {
        return cmd.contains(">>")
    }

    private fun redirectTargets(cmd: String): List<String> {
        val out = mutableListOf<String>()
        val re = Regex("(?<![>0-9])>\\s*([^\\s|;&]+)")
        re.findAll(cmd).forEach { m -> out += m.groupValues[1] }
        return out
    }

    private fun appendRedirectTargets(cmd: String): List<String> {
        val out = mutableListOf<String>()
        val re = Regex(">>\\s*([^\\s|;&]+)")
        re.findAll(cmd).forEach { m -> out += m.groupValues[1] }
        return out
    }

    private fun containsPipeToShell(cmd: String): Boolean {
        val re = Regex("\\b(curl|wget)\\b[^|]*\\|\\s*(sh|bash|zsh)\\b")
        return re.containsMatchIn(cmd)
    }

    private fun CommandRiskLevel.coerceAtLeast(other: CommandRiskLevel): CommandRiskLevel =
        if (this.ordinal >= other.ordinal) this else other
}
