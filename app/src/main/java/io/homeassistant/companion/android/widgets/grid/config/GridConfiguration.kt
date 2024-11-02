package io.homeassistant.companion.android.widgets.grid.config

data class GridConfiguration(
    val serverId: Int? = null,
    val label: String? = null,
    val requireAuthentication: Boolean = false,
    val items: List<GridItem> = emptyList()
)

data class GridItem(
    val label: String = "",
    val icon: String = "",
    val domain: String = "",
    val service: String = "",
    val serviceData: String = "",
    val id: Int = 0
)
