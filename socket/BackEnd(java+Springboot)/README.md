# DglabSocketBackEndDemo
Java版 基于SpringBoot框架的 DG-LAB socket协议服务Demo

##  项目说明
自从DG_LAB新版的V3出来，就在改自己基于V2的协议，发现改了很多东西。
问了官方，他们说从`硬件V3版本、APP3.2.0开始支持socket协议`！这真是个振奋人心的消息，
意味着我不用再改旧的蓝牙协议了，只需要维护socket消息就好，而且官方也提供了socket协议的demo，我只需要照着改就好了。
由于我用java比node更熟悉，而且java更有利于制作基于web的服务，于是就有了本项目的demo。

由于是从官方抄来的作业，[官方Git文档点这里](https://github.com/DG-LAB-OPENSOURCE/DG-LAB-OPENSOURCE/tree/main/socket)，
几乎逻辑都是照搬过来的，只有一点点不同是因为js和java的语法差异导致的细微差别。

##  项目结构
```
DglabSocketBackEndDemo
├─src
│  ├─main
│  │  ├─java       服务器端代码
│  │  └─resources  静态资源文件
```