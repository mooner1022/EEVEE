package dev.mooner.eevee.view.drawer

import androidx.annotation.DrawableRes

sealed class DrawerItem(
    open val id: Int,
    open val title: String,
    @DrawableRes open val iconRes: Int? = null,
    open val isClickable: Boolean = true
)

data class SwitchDrawerItem(
    override val id: Int,
    override val title: String,
    @DrawableRes override val iconRes: Int? = null,
    val value: String? = null,
    val switchState: Boolean = false,
    override val isClickable: Boolean = true,
    val onSwitchToggle: ((SwitchDrawerItem, Boolean) -> Unit)? = null
) : DrawerItem(id, title, iconRes, isClickable)

data class ArrowDrawerItem(
    override val id: Int,
    override val title: String,
    val value: String? = null,
    @DrawableRes override val iconRes: Int? = null,
    override val isClickable: Boolean = true,
    val onItemClick: ((ArrowDrawerItem) -> Unit)? = null
) : DrawerItem(id, title, iconRes, isClickable)

data class TextDrawerItem(
    override val id: Int,
    override val title: String,
    @DrawableRes override val iconRes: Int? = null,
    val detailText: String,
    override val isClickable: Boolean = true,
    val onItemClick: ((TextDrawerItem) -> Unit)? = null
) : DrawerItem(id, title, iconRes, isClickable)

data class HeaderDrawerItem(
    override val id: Int,
    override val title: String,
    @DrawableRes override val iconRes: Int? = null,
    override val isClickable: Boolean = false
) : DrawerItem(id, title, iconRes, isClickable)