package im.vector.riotredesign.features.home

import android.content.Context
import android.widget.ProgressBar
import com.airbnb.epoxy.ModelView

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class LoadingItem(context: Context) : ProgressBar(context)