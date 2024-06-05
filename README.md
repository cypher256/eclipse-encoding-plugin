# Autodetect Encoding Eclipse Plugin
[![GitHub Sponsor](https://img.shields.io/static/v1?label=Buy%20me%20a%20coffee&message=%E2%9D%A4&logo=coffeescript&color=orange)](https://github.com/sponsors/cypher256)

Show file information for the active editor in the Eclipse status bar.  

## Install
**First way**  
Eclipse > Help > Eclipse Marketplace... > Search "Autodetect Encoding"  
https://marketplace.eclipse.org/content/autodetect-encoding

**Second way**  
[Marketplace Web Page](https://marketplace.eclipse.org/content/autodetect-encoding)  
<!--
**Update Site**  
Help > Install New Software...  
https://raw.githubusercontent.com/cypher256/eclipse-encoding-plugin/master/eclipse.encoding.plugin.update/site.xml
-->

## Features
**Show file encoding and line ending**  
Line ending support showing CRLF, CR, LF and Mixed, can also be converted.  
![](image/ending_select.jpg)  

**Change "Text File Encoding" setting by file property**  
To add the encoding, use Preference > General > Workspace > Text File Encoding > Other.  
![](image/encoding_select.jpg)  
Autodetect: Automatic detection by juniversalchardet and ICU4J  
Inheritance: Inherited from folder or project properties, workspace preferences  
Content Type: Determined from content type setting  

'Autodetect: Set Automatically'  
This only applies if the file properties encoding is not specified.  
An automatically set is not enabled by default.

Add/Remove BOM is disable on Windows.

## License
[Eclipse Public License - v 1.0](https://www.eclipse.org/legal/epl-v10.html)  
Copyright (c) 2016- Shinji Kashihara and others. All rights reserved.  
Original source: [ystsoi/eclipse-fileencodinginfo](https://github.com/ystsoi/eclipse-fileencodinginfo)

<a href="http://with-eclipse.github.io/" target="_blank">
<img alt="with-Eclipse logo" src="http://with-eclipse.github.io/with-eclipse-0.jpg" />
</a>
