# ![image](https://github.com/user-attachments/assets/a0fe83fe-7e06-43f9-aa02-4aecf2e0fca4) Quad9 Connect


Quad9 Connect is an Android and ChromeOS app that sends your DNS queries to [Quad9's secure, private, open recursive DNS service](https://quad9.net/).

Quad9 Connect adds additional functionality and features compared to using Android's Private DNS feature to utilize encrypted DNS or configured Quad9 in Chrome on ChromeOS.

The application creates a split VPN tunnel with Android API "VpnService" to configure an alternate DNS server on Android and ChromeOS.

## Features
- DNS over TLS or DNS over UDP
- DNS Query Log
- Trusted Networks
- Blocked Query Notifications
- Local Domains
- Excluded Applications

## Third-party Software

Quad9 connect uses the following third-party software:

[Pcap4J](https://github.com/kaitoy/pcap4j) by Kaito Yamada - MIT  
[MiniDNS](https://github.com/MiniDNS/minidns) by Rene Treffer - WTFPL  
[acra](https://github.com/ACRA/acra) by ACRA - Apache  
[conscrypt](https://github.com/google/conscrypt) by google -  Apache  
[AppIntro](https://github.com/AppIntro/AppIntro) by AppIntro - Apache

## Credits

- [Po Yuan Su](https://www.linkedin.com/in/po-yuan-su-23a53b173/) - Original Author
- [Ensar Sarajčić](https://github.com/esensar) - Developer

## License

[AGPL-3.0](./LICENSE)
