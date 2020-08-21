package im.vector.app.core.ui.list

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

/**
 * A generic list item header left aligned with notice color.
 */
@EpoxyModelClass(layout = R.layout.item_generic_loader)
abstract class GenericLoaderItem : VectorEpoxyModel<GenericLoaderItem.Holder>() {

    // Maybe/Later add some style configuration, SMALL/BIG ?

    class Holder : VectorEpoxyHolder()
}
