import WidgetKit
import SwiftUI

struct MediumWidgetView: View {
  let entry: KazkarEntry

  var body: some View {
    HStack(alignment: .top, spacing: 12) {
      VStack(alignment: .leading, spacing: 6) {
        Text(entry.title)
          .font(.system(size: 16, weight: .bold))
          .foregroundColor(.primary)
          .lineLimit(2)
        Text(entry.snippet)
          .font(.system(size: 12))
          .foregroundColor(.secondary)
          .lineLimit(5)
      }
      Spacer()
    }
    .padding(14)
    .widgetURL(entry.storyId.flatMap { URL(string: "kazka://story/\($0)") })
  }
}
