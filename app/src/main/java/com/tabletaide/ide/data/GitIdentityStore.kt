package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS = "kinetic_git_identity"
private const val KEY_NAME = "name"
private const val KEY_EMAIL = "email"

@Singleton
class GitIdentityStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): GitIdentity {
        return GitIdentity(
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
        )
    }

    fun save(identity: GitIdentity) {
        prefs.edit()
            .putString(KEY_NAME, identity.name.trim())
            .putString(KEY_EMAIL, identity.email.trim())
            .apply()
    }
}
