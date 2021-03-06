package com.ruanmeng.village_arrival

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract
import android.support.design.widget.TabLayout
import android.text.InputFilter
import android.view.View
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.lzg.extend.BaseResponse
import com.lzg.extend.StringDialogCallback
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.ruanmeng.base.*
import com.ruanmeng.model.CommonData
import com.ruanmeng.model.LocationMessageEvent
import com.ruanmeng.model.RefreshMessageEvent
import com.ruanmeng.share.BaseHttp
import com.ruanmeng.utils.*
import kotlinx.android.synthetic.main.activity_address_add.*
import kotlinx.android.synthetic.main.layout_title.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.ex.loadmore.SimpleLoadMoreViewCreator
import net.idik.lib.slimadapter.ex.loadmore.SlimMoreLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class AddressAddActivity : BaseActivity() {

    private lateinit var aMap: AMap
    private var centerLatLng: LatLng? = null

    private lateinit var query: PoiSearch.Query
    private lateinit var poiSearch: PoiSearch
    private lateinit var geocoderSearch: GeocodeSearch

    private var list = ArrayList<Any>()
    private var listAddress = ArrayList<Any>()
    private lateinit var mTitle: String
    private var province = ""
    private var city = ""
    private var district = ""
    private var township = ""
    private var address = ""
    private var lat = ""
    private var lng = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_add)
        address_map.onCreate(savedInstanceState)
        mTitle = intent.getStringExtra("title")
        init_title(mTitle)

        EventBus.getDefault().register(this@AddressAddActivity)

        window.decorView.postDelayed({
            runOnUiThread {
                aMap.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {

                    override fun onCameraChangeFinish(position: CameraPosition) {
                        address_center.startJumpAnimator(100f)

                        geocoderSearch.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(
                                position.target.latitude, position.target.longitude),
                                100f,
                                GeocodeSearch.AMAP))

                        pageNum = 1
                        getData(pageNum)
                    }

                    override fun onCameraChange(position: CameraPosition) {}

                })

                address_expand.expand()
                if (!isDestroyed) getData(pageNum)
            }
        }, 2000)
    }

    @Suppress("DEPRECATION")
    override fun init_title() {
        super.init_title()
        nav_right.visibility = View.VISIBLE
        et_name.filters = arrayOf<InputFilter>(NameLengthFilter(12))
        aMap = address_map.map

        aMap.uiSettings.apply {
            isScaleControlsEnabled = false    //比例尺
            isZoomControlsEnabled = false     //缩放按钮
            isCompassEnabled = false          //指南针
            isMyLocationButtonEnabled = false //定位按钮
            isTiltGesturesEnabled = false     //倾斜手势
            isRotateGesturesEnabled = false   //旋转手势
            setLogoBottomMargin(-50)          //隐藏logo
        }

        aMap.apply {
            isTrafficEnabled = false       //实时交通状况
            mapType = AMap.MAP_TYPE_NORMAL //矢量地图模式
            isMyLocationEnabled = true     //触发定位并显示定位层
            showIndoorMap(true)            //设置是否显示室内地图
            moveCamera(CameraUpdateFactory.zoomTo(17f)) //缩放级别

            myLocationStyle = MyLocationStyle().apply {
                //定位一次，且将视角移动到地图中心点
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE)
                strokeColor(Color.TRANSPARENT)     //设置定位蓝点精度圆圈的边框颜色
                radiusFillColor(Color.TRANSPARENT) //设置定位蓝点精度圆圈的填充颜色
                showMyLocation(true)               //设置是否显示定位小蓝点
                //设置定位蓝点的icon图标方法
                myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.gps_point))
                setOnMyLocationChangeListener {
                    centerLatLng = LatLng(it.latitude, it.longitude)

                    geocoderSearch.getFromLocationAsyn(RegeocodeQuery(
                            LatLonPoint(it.latitude, it.longitude),
                            100f,
                            GeocodeSearch.AMAP))
                }
            }

            // 地图加载完成监听接口
            /*setOnMapLoadedListener {
                val latLng = aMap.cameraPosition.target
                val screenPosition = aMap.projection.toScreenLocation(latLng)
                val screenMarker = aMap.addMarker(MarkerOptions()
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.index_pos_small)))
                //设置Marker在屏幕上,不跟随地图移动
                screenMarker.setPositionByPixels(screenPosition.x, screenPosition.y)
            }*/

            setOnMapTouchListener { address_card.goneAnimation() }
        }

        //周边搜索
        initPoiSearch()

        //逆向地理编码
        geocoderSearch = GeocodeSearch(baseContext)
        geocoderSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {

            override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                if (code == AMapException.CODE_AMAP_SUCCESS) {
                    if (result?.regeocodeAddress != null
                            && result.regeocodeAddress.formatAddress != null) {

                        province = result.regeocodeAddress.province
                        city = result.regeocodeAddress.city
                        district = result.regeocodeAddress.district
                        township = result.regeocodeAddress.township
                    }
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult, code: Int) {}

        })

        address_list.load_Linear(baseContext)
        mAdapter = SlimAdapter.create()
                .register<PoiItem>(R.layout.item_map_list) { data, injector ->
                    injector.text(R.id.item_map_name, data.title)
                    injector.text(R.id.item_map_hint, data.snippet)

                            .clicked(R.id.item_map) {
                                address_card.visible()

                                address = data.title
                                lat = data.latLonPoint.latitude.toString()
                                lng = data.latLonPoint.longitude.toString()
                                township = ""
                                address_title.text = data.title
                                et_detail.setText("")

                                window.decorView.post {
                                    geocoderSearch.getFromLocationAsyn(RegeocodeQuery(
                                            LatLonPoint(data.latLonPoint.latitude, data.latLonPoint.longitude),
                                            100f,
                                            GeocodeSearch.AMAP))
                                }
                            }
                }
                .register<CommonData>(R.layout.item_map_list) { data, injector ->
                    injector.text(R.id.item_map_name, data.address + data.detailAdress)
                    injector.text(R.id.item_map_hint, "${data.name}    ${data.mobile}")

                            .clicked(R.id.item_map) {
                                EventBus.getDefault().post(LocationMessageEvent(
                                        mTitle,
                                        data.commonAddressId,
                                        data.address, data.detailAdress,
                                        data.name, data.mobile,
                                        data.lat, data.lng))
                                ActivityStack.screenManager.popActivities(this@AddressAddActivity::class.java)
                            }
                }
                .enableLoadMore(object : SlimMoreLoader(
                        baseContext,
                        SimpleLoadMoreViewCreator(baseContext)
                                .setLoadingHint("正在加载...")
                                .setNoMoreHint("没有更多数据了")
                                .setPullToLoadMoreHint("加载更多")
                                .setErrorHint("加载失败")) {
                    override fun hasMore(): Boolean = mPosition == 0
                    override fun onLoadMore(handler: Handler) = getData(pageNum)
                })
                .attachTo(address_list)

        when (mTitle) {
            "我的地址" -> {
                address_tab.apply {
                    setSelectedTabIndicatorColor(resources.getColor(R.color.white))
                    addTab(this.newTab().setText("附近地址"), true)
                }
            }
            else -> {
                address_tab.apply {
                    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                        override fun onTabReselected(tab: TabLayout.Tab) {}
                        override fun onTabUnselected(tab: TabLayout.Tab) {}

                        override fun onTabSelected(tab: TabLayout.Tab) {
                            address_card.goneAnimation()
                            mPosition = tab.position
                            when (tab.position) {
                                1 -> mAdapter.updateData(list)
                                0 -> {
                                    if (listAddress.isEmpty()) getAddressData()
                                    else mAdapter.updateData(listAddress)
                                }
                            }
                        }

                    })

                    addTab(this.newTab().setText("常用地址"), true)
                    addTab(this.newTab().setText("附近地址"), false)

                    post { Tools.setIndicator(this, 40, 40) }
                }
            }
        }

        nav_right.setOnClickListener {

            if (address_card.visibility != View.VISIBLE) return@setOnClickListener

            if (township.isEmpty()) {
                showToast("地址信息获取失败")
                return@setOnClickListener
            }

            if (mTitle == "收货地址" && et_name.text.isEmpty()) {
                et_name.requestFocus()
                showToast("请输入姓名")
                return@setOnClickListener
            }

            if (mTitle == "收货地址" && et_tel.text.isEmpty()) {
                et_tel.requestFocus()
                showToast("请输入手机号")
                return@setOnClickListener
            }

            /*if (mTitle != "购买地址" && !CommonUtil.isMobile(et_tel.text.toString())) {
                et_tel.requestFocus()
                et_tel.setText("")
                showToast("手机号码格式错误，请重新输入")
                return@setOnClickListener
            }*/

            if (mTitle == "我的地址") {
                OkGo.post<String>(BaseHttp.add_commonaddress)
                        .tag(this@AddressAddActivity)
                        .isMultipart(true)
                        .headers("token", getString("token"))
                        .params("type", "0")
                        .params("lat", lat)
                        .params("lng", lng)
                        .params("province", province)
                        .params("city", city)
                        .params("district", district)
                        .params("township", township)
                        .params("address", address)
                        .params("detailAdress", et_detail.text.toString())
                        .params("name", et_name.text.toString())
                        .params("mobile", et_tel.text.toString())
                        .execute(object : StringDialogCallback(baseContext) {

                            override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                                showToast(msg)
                                EventBus.getDefault().post(RefreshMessageEvent("新增地址"))
                                ActivityStack.screenManager.popActivities(this@AddressAddActivity::class.java)
                            }

                        })
            } else {
                EventBus.getDefault().post(LocationMessageEvent(
                        mTitle, "",
                        address, et_detail.text.toString(),
                        et_name.text.toString(), et_tel.text.toString(),
                        lat, lng,
                        province, city, district, township))
                ActivityStack.screenManager.popActivities(this@AddressAddActivity::class.java)
            }
        }

        address_img.setOnClickListener {
            startActivityForResult(Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI),
                    10)
        }

        address_title.setOnClickListener { /*防止点击隐藏*/ }
    }

    private fun initPoiSearch() {
        query = PoiSearch.Query("", "", "")
        query.pageSize = 20
        poiSearch = PoiSearch(this, query)
        poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {

            override fun onPoiItemSearched(poiItem: PoiItem, code: Int) {}

            override fun onPoiSearched(result: PoiResult?, code: Int) {
                cancelLoadingDialog()
                if (code != AMapException.CODE_AMAP_SUCCESS) return

                if (result != null && result.query != null) {
                    if (pageNum == 1) {
                        if (list.isNotEmpty()) address_list.scrollToPosition(0)
                        list.clear()
                    }
                    if (result.pois.isNotEmpty()) pageNum++
                    list.addItems(result.pois)

                    if (mPosition == 0 && mTitle != "我的地址") return
                    mAdapter.updateData(list)

                    empty_view.apply { if (list.isEmpty()) visible() else gone() }
                }
            }

        })
    }

    override fun getData(pindex: Int) {
        val latLng = aMap.cameraPosition.target

        if (pindex == 1) showLoadingDialog()
        query.pageNum = pindex
        poiSearch.bound = PoiSearch.SearchBound(LatLonPoint(latLng.latitude, latLng.longitude), 20000)
        poiSearch.searchPOIAsyn()
    }

    private fun getAddressData() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.my_commonaddress_list)
                .tag(this@AddressAddActivity)
                .headers("token", getString("token"))
                .params("type", 1)
                .execute(object : JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(baseContext) {

                    override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {

                        listAddress.apply {
                            clear()
                            addItems(response.body().`object`)
                        }

                        if (mPosition == 0) mAdapter.updateData(listAddress)
                    }

                })
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.address_location -> {
                if (centerLatLng != null)
                    aMap.animateCamera(CameraUpdateFactory.changeLatLng(centerLatLng))
            }
            R.id.address_search -> {
                val intent = Intent(baseContext, AddressSearchActivity::class.java)
                intent.putExtra("title", "选择地址")
                intent.putExtra("city", city)
                startActivity(intent)
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 10) {
            val reContentResolver = contentResolver
            val cursor = managedQuery(data?.data, null, null, null, null)
            cursor.moveToFirst()
            val userName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            val phone = reContentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null,
                    null)
            while (phone.moveToNext()) {
                val userNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                et_name.setText(userName)
                et_tel.setText(userNumber.replace("-", "")
                        .replace(" ", ""))
            }
        }
    }

    override fun finish() {
        EventBus.getDefault().unregister(this@AddressAddActivity)
        super.finish()
    }

    @Subscribe
    fun onMessageEvent(event: LocationMessageEvent) {
        when (event.type) {
            "选择地址" -> {
                aMap.animateCamera(CameraUpdateFactory.changeLatLng(LatLng(
                        event.lat.toDouble(),
                        event.lng.toDouble())))

                address_card.visible()
                address = event.address
                lat = event.lat
                lng = event.lng
                township = ""
                address_title.text = address
                et_detail.setText("")

                window.decorView.post {
                    geocoderSearch.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(
                            lat.toDouble(), lng.toDouble()),
                            100f,
                            GeocodeSearch.AMAP))
                }
            }
        }
    }
}
