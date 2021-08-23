package io.homeassistant.companion.android.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.onboarding.viewHolders.HeaderViewHolder
import io.homeassistant.companion.android.onboarding.viewHolders.InstanceViewHolder
import io.homeassistant.companion.android.onboarding.viewHolders.LoadingViewHolder
import io.homeassistant.companion.android.onboarding.viewHolders.ManualSetupViewHolder
import kotlin.math.min

class ServerListAdapter(
    val servers: ArrayList<HomeAssistantInstance>
) : RecyclerView.Adapter<ViewHolder>() {

    lateinit var onInstanceClicked: (HomeAssistantInstance) -> Unit
    lateinit var onManualSetupClicked: () -> Unit

    companion object {
        private const val TYPE_INSTANCE = 1
        private const val TYPE_HEADER = 2
        private const val TYPE_LOADING = 3
        private const val TYPE_MANUAL = 4
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return when (viewType) {
            TYPE_INSTANCE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.listitem_instance, parent, false)
                InstanceViewHolder(view, onInstanceClicked)
            }
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.listitem_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_MANUAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.listitem_instance, parent, false)
                ManualSetupViewHolder(view, onManualSetupClicked)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.listitem_loading, parent, false)
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is InstanceViewHolder && position <= servers.size) {
            holder.server = servers[position - 1]
        } else if (holder is ManualSetupViewHolder) {
            holder.text.setText(R.string.manual_setup)
        } else if (holder is HeaderViewHolder) {
            holder.headerTextView.setText(R.string.list_header_instances)
        }
    }

    override fun getItemCount() = min(servers.size + 2, 3)

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 -> TYPE_HEADER
            position == this.itemCount - 1 -> TYPE_MANUAL
            servers.size > 0 -> TYPE_INSTANCE
            else -> TYPE_LOADING
        }
    }
}
