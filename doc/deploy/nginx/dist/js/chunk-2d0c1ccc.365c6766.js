(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-2d0c1ccc"],{"481c":function(e,t,a){"use strict";a.r(t);var s=function(){var e=this,t=e.$createElement,a=e._self._c||t;return a("page-header-wrapper",[a("a-space",{staticStyle:{width:"100%"},attrs:{direction:"vertical"}},[a("a-row",{attrs:{gutter:24}},[a("a-col",{attrs:{span:12}},[a("a-card",{attrs:{loading:e.loading,title:"CPU",bordered:!1}},[e.server.cpu?a("a-row",{attrs:{gutter:24}},[a("a-col",{attrs:{span:12}},[a("a-space",{attrs:{direction:"vertical",size:"large"}},[a("a-statistic",{staticStyle:{"margin-right":"50px"},attrs:{title:"核心数",value:e.server.cpu.cpuNum,"value-style":{color:"#3f8600"}},scopedSlots:e._u([{key:"prefix",fn:function(){return[a("a-icon",{attrs:{type:"setting"}})]},proxy:!0}],null,!1,3346964861)}),a("a-statistic",{staticStyle:{"margin-right":"50px"},attrs:{title:"用户使用率",value:e.server.cpu.used,precision:2,suffix:"%","value-style":{color:"#3f8600"}},scopedSlots:e._u([{key:"prefix",fn:function(){return[a("a-icon",{attrs:{type:"team"}})]},proxy:!0}],null,!1,3016208662)})],1)],1),a("a-col",{attrs:{span:12}},[a("a-space",{attrs:{direction:"vertical",size:"large"}},[a("a-statistic",{staticStyle:{"margin-right":"50px"},attrs:{title:"系统使用率",value:e.server.cpu.sys,precision:2,suffix:"%","value-style":{color:"#3f8600"}},scopedSlots:e._u([{key:"prefix",fn:function(){return[a("a-icon",{attrs:{type:"cloud-server"}})]},proxy:!0}],null,!1,4232312562)}),a("a-statistic",{staticStyle:{"margin-right":"50px"},attrs:{title:"当前空闲率",value:e.server.cpu.free,precision:2,suffix:"%","value-style":{color:"#3f8600"}},scopedSlots:e._u([{key:"prefix",fn:function(){return[a("a-icon",{attrs:{type:"inbox"}})]},proxy:!0}],null,!1,789749945)})],1)],1)],1):e._e()],1)],1),a("a-col",{attrs:{span:12}},[a("a-card",{attrs:{loading:e.loading,title:"内存",bordered:!1}},[a("a-table",{attrs:{loading:e.loading,size:"small",rowKey:"name",columns:e.memColumns,"data-source":e.memData,pagination:!1},scopedSlots:e._u([{key:"mem",fn:function(t,s){return a("span",{},[a("div",{style:{color:"使用率"==s.name&&t>80?"red":""}},["使用率"==s.name&&t>80?a("a-icon",{staticStyle:{color:"#FFCC00"},attrs:{type:"warning"}}):e._e(),e._v(" "+e._s(t)+"   "),"使用率"==s.name?a("code",[e._v("%")]):e._e(),"使用率"!=s.name?a("code",[e._v("G")]):e._e()],1)])}},{key:"jvm",fn:function(t,s){return a("span",{},[a("div",{style:{color:"使用率"==s.name&&t>80?"red":""}},["使用率"==s.name&&t>80?a("a-icon",{staticStyle:{color:"#FFCC00"},attrs:{type:"warning"}}):e._e(),e._v(" "+e._s(t)+"   "),"使用率"==s.name?a("code",[e._v("%")]):e._e(),"使用率"!=s.name?a("code",[e._v("M")]):e._e()],1)])}}])})],1)],1)],1),a("a-row",{attrs:{gutter:24}},[a("a-col",{attrs:{span:24}},[a("a-card",{attrs:{loading:e.loading,title:"服务器信息",bordered:!1}},[e.server.sys?a("a-descriptions",{attrs:{column:2}},[a("a-descriptions-item",{attrs:{label:"服务器名称"}},[e._v(" "+e._s(e.server.sys.computerName)+" ")]),a("a-descriptions-item",{attrs:{label:"操作系统"}},[e._v(" "+e._s(e.server.sys.osName)+" ")]),a("a-descriptions-item",{attrs:{label:"服务器IP"}},[e._v(" "+e._s(e.server.sys.computerIp)+" ")]),a("a-descriptions-item",{attrs:{label:"系统架构"}},[e._v(" "+e._s(e.server.sys.osArch)+" ")])],1):e._e()],1)],1)],1),a("a-row",{attrs:{gutter:24}},[a("a-col",{attrs:{span:24}},[a("a-card",{attrs:{loading:e.loading,title:"Java虚拟机信息",bordered:!1}},[e.server.jvm?a("a-descriptions",{attrs:{column:2}},[a("a-descriptions-item",{attrs:{label:"Java名称"}},[e._v(" "+e._s(e.server.jvm.name)+" ")]),a("a-descriptions-item",{attrs:{label:"Java版本"}},[e._v(" "+e._s(e.server.jvm.version)+" ")]),a("a-descriptions-item",{attrs:{label:"启动时间"}},[e._v(" "+e._s(e.server.jvm.startTime)+" ")]),a("a-descriptions-item",{attrs:{label:"运行时长"}},[e._v(" "+e._s(e.server.jvm.runTime)+" ")]),a("a-descriptions-item",{attrs:{label:"安装路径"}},[e._v(" "+e._s(e.server.jvm.home)+" ")]),a("a-descriptions-item",{attrs:{label:"项目路径"}},[e._v(" "+e._s(e.server.sys.userDir)+" ")])],1):e._e()],1)],1)],1),a("a-row",{attrs:{gutter:24}},[a("a-col",{attrs:{span:24}},[a("a-card",{attrs:{loading:e.loading,title:"磁盘状态",bordered:!1}},[a("a-table",{attrs:{loading:e.loading,size:e.tableSize,rowKey:"dirName",columns:e.sysColumns,"data-source":e.sysData,pagination:!1,bordered:e.tableBordered},scopedSlots:e._u([{key:"total",fn:function(t){return a("span",{},[e._v(" "+e._s(t)+" ")])}},{key:"free",fn:function(t){return a("span",{},[e._v(" "+e._s(t)+" ")])}},{key:"used",fn:function(t){return a("span",{},[e._v(" "+e._s(t)+" ")])}},{key:"usage",fn:function(t){return a("span",{},[a("div",{style:{color:t>80?"red":""}},[t>80?a("a-icon",{staticStyle:{color:"#FFCC00"},attrs:{type:"warning"}}):e._e(),e._v(" "+e._s(t)),a("code",[e._v("%")])],1)])}}])})],1)],1)],1)],1)],1)},r=[],n=a("b775");function i(){return Object(n["b"])({url:"/admin/v1/monitors/server",method:"get"})}var o=a("435a"),l={name:"Server",mixins:[o["a"]],data:function(){return{server:[],loading:!0,memColumns:[{title:"属性",dataIndex:"name"},{title:"内存",dataIndex:"mem",scopedSlots:{customRender:"mem"}},{title:"JVM",dataIndex:"jvm",scopedSlots:{customRender:"jvm"}}],memData:[],sysColumns:[{title:"盘符路径",dataIndex:"dirName",ellipsis:!0},{title:"文件系统",dataIndex:"sysTypeName"},{title:"盘符类型",dataIndex:"typeName",ellipsis:!0},{title:"总大小",dataIndex:"total",scopedSlots:{customRender:"total"}},{title:"可用大小",dataIndex:"free",scopedSlots:{customRender:"free"}},{title:"已用大小",dataIndex:"used",scopedSlots:{customRender:"used"}},{title:"已用百分比",dataIndex:"usage",scopedSlots:{customRender:"usage"}}],sysData:[]}},filters:{},created:function(){this.getList()},computed:{},watch:{},methods:{getList:function(){var e=this;i().then((function(t){var a=t.data;e.server=a,e.memData=[{name:"总内存",mem:a.mem.total,jvm:a.jvm.total},{name:"已用内存",mem:a.mem.used,jvm:a.jvm.used},{name:"剩余内存",mem:a.mem.free,jvm:a.jvm.free},{name:"使用率",mem:a.mem.usage,jvm:a.jvm.usage}],e.sysData=a.files,setTimeout((function(){e.loading=!1}),500)}))}}},c=l,d=a("2877"),m=Object(d["a"])(c,s,r,!1,null,null,null);t["default"]=m.exports}}]);