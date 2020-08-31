Habits
=====
An Android habit-tracking app

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width=200 />](https://play.google.com/store/apps/details?id=com.dwett.habits&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)

![Screenshot](/screenshot.png?raw=true "Screenshot")

## Privacy
- This app never sends any of your data anywhere (currently, there are no network calls in the
  codebase!).
- I will never add ads to this application.
- I will never collect or send your data anywhere.
- If the app asks for an unexpected permission please submit an issue! It should only ask
  to store data locally and to send you a reminder location (see TODO below to let this be disabled).

## Controls
- Long press on a habit to enter the "edit" menu for it. This view will show a list of "events" for each habit.
- Tap on an event's date or time to edit when it happened.
- On the Summary page, you can tap an "archived" habit to un-archive it.
- Use the back button to navigate to previous menus or exit the app.

## Detailed Features
- Ability to add, and edit habits with customizable frequency (per week)
- Daily push notification reminders to fill out habit status (currently at 11pm, see TODO below)
- Summary view to track your habit formation
- Color-coded progress bars on habits to identify which ones you're behind on.
- Ability to archive habits after they've formed, or if you want to stop tracking them for now.
- Ability to tweak (add, adjust time, or delete) all logged events (in case you forget or accidentally tap).
- Log events marked "Done" near midnight to the previous day (see TODO below about making this tunable).
- Ability to export or import all information from the app to and from a CSV (You have to copy-paste it for now)

## TODO
- Tests :)
- Move DB calls off the main thread. Looks like this requires some "LiveData", but it seems
  tolerable on my device, even after many months or daily usage.
- Pagination of Summary page. Right now it returns the most recent 6 weeks, but I need to let it
  fetch more if you scroll to the bottom.
- A settings page! First up: configurable reminder notification time and near-midnight thresholds.
- Next setting up: The ability to disable notifications
- Add copy button on export view, or copy automatically.
