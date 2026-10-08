package com.abilas.connecthub

import androidx.annotation.IdRes
import androidx.annotation.StringRes

/** The bottom navigation destinations. Every one except [START] is a web view. */
enum class Section(@IdRes val menuId: Int, @StringRes val label: Int) {
    START(R.id.nav_start, R.string.nav_start),
    DISCOVER(R.id.nav_discover, R.string.nav_discover),
    STORE(R.id.nav_store, R.string.nav_store),
    ACCOUNT(R.id.nav_account, R.string.nav_account),
    HELP(R.id.nav_help, R.string.nav_help);

    fun homeUrl(links: UbisoftLinks): String? = when (this) {
        START -> null
        DISCOVER -> links.news
        STORE -> links.storeHome
        ACCOUNT -> links.account
        HELP -> links.help
    }

    companion object {
        fun forMenuId(@IdRes id: Int): Section = entries.first { it.menuId == id }
    }
}
