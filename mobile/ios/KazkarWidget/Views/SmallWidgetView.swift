import WidgetKit
import SwiftUI

struct SmallWidgetView: View {
  let entry: KazkarEntry

  var body: some View {
    VStack(alignment: .leading, spacing: 4) {
      Text(entry.title)
        .font(.system(size: 14, weight: .bold))
        .foregroundColor(.primary)
        .lineLimit(2)
      Spacer()
      Text(entry.snippet)
        .font(.system(size: 11))
        .foregroundColor(.secondary)
        .lineLimit(3)
    }
    .padding(12)
    .widgetURL(entry.storyId.flatMap { URL(string: "kazka://story/\($0)") })
  }
}
