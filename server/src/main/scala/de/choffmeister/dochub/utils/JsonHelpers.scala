package de.choffmeister.dochub.utils

import play.api.libs.json.{Format, JsValue}

object JsonHelpers {
  def mappedFormat[W, V](wrap: V => W, unwrap: W => V)(implicit valueFormat: Format[V]): Format[W] = {
    Format[W]((json: JsValue) => valueFormat.reads(json).map(wrap), (wrapped: W) => valueFormat.writes(unwrap(wrapped)))
  }
}
