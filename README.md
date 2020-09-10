# 从零开始的RPC
## 0. 项目背景
因为是大家都说秒杀项目不好，所以这段日子一直纠结要再做一个什么项目来完善一下简历，物色了两个项目。其中仿牛客网项目看了一下技术栈，好像对自身并没有多大的提升(感觉就是我的博客项目 + 秒杀)。近些日子复习的压力也很大，所以决定做一个造轮子的项目，主要原因有两点：第一，自己对于IO方面知识了解还不够深入，借着学习Netty再次深入学习IO。第二，造轮子项目相较于web项目，不用写前端，项目的架构也简单很多，耗时更少。现在忙于秋招复习，知识点和算法的复习仍旧是主要工作。

## 1. 项目介绍
从0开始，用Netty实现一个RPC框架，经历多个版本更迭，功能逐渐完善，虽然还有很多功能没有实现。但是跟着版本文档来写，应该会很顺利地上手。

## 2. 项目技术栈：Netty、Json序列化、zookeeper注册中心

## 3. 知识背景
1. RPC相关知识：

2. Dubbo基本功能
  1. 远程服务调用
  2. 负载均衡
  3. 服务注册中心
  
## 4. 版本更替：
1. [version0](https://github.com/ks-39/MyRPC/blob/master/RPC_Version0%E2%80%94%E2%80%94BIO%E4%BC%AA%E5%BC%82%E6%AD%A5%E6%A8%A1%E5%9E%8B.md)：BIO伪异步模型
2. [version1](https://github.com/ks-39/MyRPC/blob/master/RPC_Version1%E2%80%94%E2%80%94%E4%BD%BF%E7%94%A8Netty%E6%A1%86%E6%9E%B6%E8%A7%A3%E5%86%B3TCP%E7%B2%98%E5%8C%85.md)：Netty基本模型 + 自定义字节流长度解决TCP粘包问题
3. [version2](https://github.com/ks-39/MyRPC/blob/master/RPC_Version2%E2%80%94%E2%80%94%E5%A4%9A%E7%A7%8D%E5%BA%8F%E5%88%97%E5%8C%96%E6%96%B9%E5%BC%8F%E5%92%8C%E8%87%AA%E5%AE%9A%E4%B9%89%E7%BC%96%E7%A0%81%E8%A7%A3%E7%A0%81.md)：多种序列化方式和自定义编码解码
4. [version3](https://github.com/ks-39/MyRPC/blob/master/RPC_Version3%E2%80%94%E2%80%94%E4%BD%BF%E7%94%A8zookeeper%E6%B3%A8%E5%86%8C%E4%B8%AD%E5%BF%83.md)：使用zookeeper注册中心
5. [version4](https://github.com/ks-39/MyRPC/blob/master/RPC_Version4%E2%80%94%E2%80%94%E8%B4%9F%E8%BD%BD%E5%9D%87%E8%A1%A1.md)：负载均衡
