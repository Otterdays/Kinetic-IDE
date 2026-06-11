package com.tabletaide.ide.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubApiParserTest {

    @Test
    fun parseUser_mapsLoginAndName() {
        val user = GitHubApiParser.parseUser(
            """
            {
              "login": "octocat",
              "name": "The Octocat",
              "avatar_url": "https://avatars.githubusercontent.com/u/1"
            }
            """.trimIndent(),
        )
        assertEquals("octocat", user.login)
        assertEquals("The Octocat", user.name)
    }

    @Test
    fun parseRepos_mapsCloneUrlAndPrivacy() {
        val repos = GitHubApiParser.parseRepos(
            """
            [
              {
                "id": 1,
                "name": "Hello-World",
                "full_name": "octocat/Hello-World",
                "private": false,
                "clone_url": "https://github.com/octocat/Hello-World.git",
                "updated_at": "2026-01-01T00:00:00Z",
                "description": "My first repo"
              }
            ]
            """.trimIndent(),
        )
        assertEquals(1, repos.size)
        assertEquals("octocat/Hello-World", repos[0].fullName)
        assertEquals("https://github.com/octocat/Hello-World.git", repos[0].cloneUrl)
        assertTrue(!repos[0].isPrivate)
    }
}
