# System tray compatibility contract

The Windows notification-area icon is part of the NexU operator workflow, not optional decoration.

The packaged desktop application must:

- enable the system tray by default;
- include `tray-icon.png`;
- initialize AWT tray components on the AWT event-dispatch thread;
- expose **About**, **Preferences**, **Show logs**, **Select language**, **Manage keystores**, extension actions and **Exit**;
- open the active `nexu.log` file with the operating system's default text-file application;
- support English and Italian desktop resources, with English selected on first start;
- persist a language selection and apply it on the next application start;
- keep the JavaFX runtime alive while the main stage is hidden;
- log successful tray initialization or the concrete reason it is unavailable.

The **Manage keystores** action is supplied by `KeystoreProductAdapter` and opens the existing JavaFX keystore-management screen. Spring Boot owns only the local HTTP server lifecycle and must not replace or remove this desktop interaction.

The **Show logs** action uses the same resolved file path configured for Logback, including portable-package log directories. The **Select language** action stores only the supported language code (`en` or `it`) in the existing user-preferences node.
