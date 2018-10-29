Habits
=====
An Android habit-tracking app

![Screenshot](/screenshot.png?raw=true "Screenshot")

## Privacy
- This app never sends any of your data anywhere (currently, there are no network calls in the
  codebase!).
- I will never add ads to this application or send your data anywhere.
- If the app asks for an unexpected permission please submit a pull request! It should only ask
  to store data locally and send you a reminder location (see TODO below to let this be diabled)

## Controls
- Long press on a habit to enter the "edit" menu for it.
- Tap on an event's date or time to edit when it happened.

## Detailed Features
- Ability to add, edit, delete habits with customizable frequency (per week)
- Daily push notification reminders to fill out habit status (currently at 11pm, see TODO below)
- Summary view to track how your habit formation is going
- Color-coded progress bars on habits to identify which ones you're behind on
- Ability to archive habits after they've formed, or if you want to ignore them for now.
- Ability to tweak all logged events (in case you forget or accidentally tap)
- Log events marked "Done" near midnight to the previous day (tunable soon, see TODO below).

## TODO
- Tests :)
- Move DB calls off the main thread. Looks like this requires some "LiveData", but it seems
  tolerable on my device
- Pagination of Summary page. Right now it returns the most recent 6 weeks, but I need to let it
  fetch more if you scroll to the bottom.
- A settings page! First up: configurable reminder notification time and near-midnight thresholds.
- Next setting up: The ability to disable notifications
- Export to a parsable format (probably CSV?)
