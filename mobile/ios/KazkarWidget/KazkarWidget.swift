import WidgetKit
import SwiftUI

struct KazkarWidget: Widget {
  let kind: String = "KazkarWidget"

  var body: some WidgetConfiguration {
    StaticConfiguration(kind: kind, provider: Provider()) { entry in
      switch entry.family {
      case .systemSmall: SmallWidgetView(entry: entry)
      case .systemMedium: MediumWidgetView(entry: entry)
      default: SmallWidgetView(entry: entry)
      }
    }
    .configurationDisplayName("Kazkar")
    .description("Your latest fairy tale at a glance.")
    .supportedFamilies([.systemSmall, .systemMedium])
  }
}
