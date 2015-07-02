#WaterDropListView
A powerful ListView with awesome pull-refresh and pull-on-loadmore function!
##PreView
![demo]( https://github.com/THEONE10211024/WaterDropListView/blob/master/demo/demo.gif)
##Usage
1.download the project    
2.import it into your project as a lib.    
3.use the “WaterDropListView” in your code,just like the normal ListView.    
###Code
1. Replace standard `ListView` with ` medusa.theone.waterdroplistview.view.WaterDropListView ` in your `layout.xml` file.    

```xml
<medusa.theone.waterdroplistview.view.WaterDropListView
           android:id="@+id/waterdrop_listview"
           android:layout_width="match_parent"
           android:layout_height="wrap_content">
</medusa.theone.waterdroplistview.view.WaterDropListView>
```
2.implements the WaterDropListView.IWaterDropListViewListener in your Activity or Fragment.    
```java
Public class MainActivity extends Activity implements WaterDropListView.IWaterDropListViewListener
```
When pulling to refresh,the ```
public void onRefresh()
```will be called.
When pulling to load moer,the ```
public void onLoadMore()
```will be called    
3.Then you can use it just like the ListView.    
##Summary
If you have any problem ,please let me know! Hope you like it!   
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-WaterDropListView-green.svg?style=flat)](https://android-arsenal.com/details/1/2078)
