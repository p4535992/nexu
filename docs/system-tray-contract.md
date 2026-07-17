# System tray compatibility contract

The Windows notification-area icon is part of the NexU operator workflow, not optional decoration.

The packaged desktop application must:

- enable the system tray by default;
- include `tray-icon.png`;
- initialize AWT tray components on the AWT event-dispatch thread;
- expose **About**, **Preferences**, **Manage keystores**, extension actions and **Exit**;
- keep the JavaFX runtime alive while the main stage is hidden;
- log successful tray initialization or the concrete reason it is unavailable.

The **Manage keystores** action is supplied by `KeystoreProductAdapter` and opens the existing JavaFX keystore-management screen. Spring Boot owns only the local HTTP server lifecycle and must not replace or remove this desktop interaction.
