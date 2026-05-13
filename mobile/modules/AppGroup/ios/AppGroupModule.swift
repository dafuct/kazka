import ExpoModulesCore
import WidgetKit

public class AppGroupModule: Module {
  public func definition() -> ModuleDefinition {
    Name("AppGroup")

    AsyncFunction("writeJSON") { (filename: String, payload: [String: Any]) throws -> Void in
      guard let container = FileManager.default
        .containerURL(forSecurityApplicationGroupIdentifier: "group.app.kazka")
      else {
        throw Exception(name: "NoAppGroup",
                        description: "App Group container not available")
      }
      let url = container.appendingPathComponent(filename)
      let data = try JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys])
      try data.write(to: url, options: .atomic)
    }

    AsyncFunction("reloadAllTimelines") { () -> Void in
      if #available(iOS 14, *) {
        WidgetCenter.shared.reloadAllTimelines()
      }
    }
  }
}
