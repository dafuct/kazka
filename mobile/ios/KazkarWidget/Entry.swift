import WidgetKit
import SwiftUI

struct KazkarEntry: TimelineEntry {
  let date: Date
  let family: WidgetFamily
  let title: String
  let snippet: String
  let storyId: String?
}
