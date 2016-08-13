# Autodetect Encoding Eclipse Plugin
Show file information for the active editor in the Eclipse status bar.  

## Install
**First way**  
Drag-n-drop the following button to your running Eclipse main toolbar.  
<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2925771" class="drag" title="Drag-n-drop to your running Eclipse main toolbar to install Autodetect Encoding plugin"><img class="img-responsive" src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png" alt="Drag-n-drop to your running Eclipse main toolbar to install Autodetect Encoding plugin" /></a>

**Second way**  
Eclipse > Help > Eclipse Marketplace... > Search "Autodetect Encoding"
<!--
**Eclipse Marketplace**  
https://marketplace.eclipse.org/content/encoding-statusbar  
Drag install button of the above site to your running Eclipse workspace to install.  

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
Autodetect: Automatic detection by juniversalchardet  
Inheritance: Inherited from folder or project properties, workspace preferences  
Content Type: Determined from content type setting  

'Autodetect: Set Automatically'  
This only applies if the file properties encoding is not specified.

## License
[Eclipse Public License - v 1.0](https://www.eclipse.org/legal/epl-v10.html)  
Copyright (c) 2016- Shinji Kashihara and others. All rights reserved.  
Original source: [ystsoi/eclipse-fileencodinginfo](https://github.com/ystsoi/eclipse-fileencodinginfo)
