import WidgetKit
import SwiftUI

struct Provider: TimelineProvider {
  static let appGroupId = "group.app.kazka"
  static let jsonFileName = "today.json"

  func placeholder(in context: Context) -> KazkarEntry {
    KazkarEntry(date: Date(), family: context.family,
                title: "Kazkar",
                snippet: "Magical tales await…",
                storyId: nil)
  }

  func getSnapshot(in context: Context, completion: @escaping (KazkarEntry) -> Void) {
    completion(load(family: context.family))
  }

  func getTimeline(in context: Context, completion: @escaping (Timeline<KazkarEntry>) -> Void) {
    let entry = load(family: context.family)
    let next = Calendar.current.date(byAdding: .hour, value: 1, to: Date()) ?? Date().addingTimeInterval(3600)
    completion(Timeline(entries: [entry], policy: .after(next)))
  }

  private func load(family: WidgetFamily) -> KazkarEntry {
    guard
      let url = FileManager.default
        .containerURL(forSecurityApplicationGroupIdentifier: Self.appGroupId)?
        .appendingPathComponent(Self.jsonFileName),
      let data = try? Data(contentsOf: url),
      let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    else {
      return KazkarEntry(date: Date(), family: family,
                         title: "Kazkar",
                         snippet: "Open the app to create your first story.",
                         storyId: nil)
    }

    let title = (payload["title"] as? String) ?? "Kazkar"
    let snippet = (payload["snippet"] as? String) ?? ""
    let storyId = payload["storyId"] as? String
    return KazkarEntry(date: Date(), family: family,
                       title: title, snippet: snippet, storyId: storyId)
  }
}
