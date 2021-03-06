package com.ruanmeng.village_arrival

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.ImageView
import com.amap.api.AMapLocationHelper
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.compress.Luban
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.lzg.extend.StringDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.ruanmeng.base.*
import com.ruanmeng.model.RefreshMessageEvent
import com.ruanmeng.share.BaseHttp
import com.ruanmeng.share.Const
import com.ruanmeng.utils.ActivityStack
import com.ruanmeng.utils.CommonUtil
import com.ruanmeng.utils.NameLengthFilter
import com.ruanmeng.view.FullyGridLayoutManager
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_live_issue.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.File

class LiveIssueActivity : BaseActivity() {

    private var selectList = ArrayList<LocalMedia>()
    private lateinit var geocoderSearch: GeocodeSearch
    private var locationLat = ""
    private var locationLng = ""
    private var locationProvince = ""
    private var locationCity = ""
    private var locationDistrict = ""
    private var locationTownship = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_issue)
        init_title("我要发布")

        startLocation()

        selectList.add(LocalMedia().apply { compressPath = "" })
        (live_list.adapter as SlimAdapter).updateData(selectList)
    }

    override fun init_title() {
        super.init_title()
        bt_issue.setBackgroundResource(R.drawable.rec_bg_d0d0d0)
        bt_issue.isClickable = false

        et_title.addTextChangedListener(this@LiveIssueActivity)
        et_content.addTextChangedListener(this@LiveIssueActivity)
        et_name.filters = arrayOf<InputFilter>(NameLengthFilter(16))

        //逆向地理编码
        geocoderSearch = GeocodeSearch(baseContext)
        geocoderSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {

            @SuppressLint("SetTextI18n")
            override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                if (code == AMapException.CODE_AMAP_SUCCESS) {
                    if (result?.regeocodeAddress != null
                            && result.regeocodeAddress.formatAddress != null) {

                        locationProvince = result.regeocodeAddress.province
                        locationCity = result.regeocodeAddress.city
                        locationDistrict = result.regeocodeAddress.district
                        locationTownship = result.regeocodeAddress.township

                        live_loc.text = locationDistrict + locationTownship
                    }
                }
            }

            override fun onGeocodeSearched(result: GeocodeResult, code: Int) {}

        })

        bt_issue.setOneClickListener(View.OnClickListener {
            if (locationTownship.isEmpty()) {
                showToast("当前位置信息获取失败")
                return@OnClickListener
            }

            if (et_tel.text.isNotEmpty() && !CommonUtil.isMobile(et_tel.text.toString())) {
                showToast("手机号码格式不正确，请重新输入")
                return@OnClickListener
            }

            OkGo.post<String>(BaseHttp.add_cooperation)
                    .tag(this@LiveIssueActivity)
                    .isMultipart(true)
                    .headers("token", getString("token"))
                    .params("lat", locationLat)
                    .params("lng", locationLng)
                    .params("province", locationProvince)
                    .params("city", locationCity)
                    .params("district", locationDistrict)
                    .params("township", locationTownship)
                    .params("title", et_title.text.trim().toString())
                    .params("content", et_content.text.trim().toString())
                    .params("type", intent.getStringExtra("type"))
                    .apply {
                        (0 until (selectList.filter { it.compressPath.isNotEmpty() }.size)).forEach {
                            params("img${it + 1}", File(selectList[it].compressPath))
                        }
                        params("mobile", et_tel.text.toString())
                        params("name", et_name.text.toString())
                    }
                    .execute(object : StringDialogCallback(baseContext) {

                        @SuppressLint("SetTextI18n")
                        override fun onSuccessResponse(response: Response<String>, msg: String, msgCode: String) {

                            val obj = JSONObject(response.body()).getJSONObject("object") ?: JSONObject()
                            val cooperationId = obj.optString("cooperationId")

                            if (cooperationId.isNotEmpty()) {
                                val intent = Intent(baseContext, LiveDoneActivity::class.java)
                                intent.putExtra("title", "发布成功")
                                intent.putExtra("cooperationId", cooperationId)
                                startActivity(intent)
                                EventBus.getDefault().post(RefreshMessageEvent("发布需求"))
                            } else showToast(msg)

                            ActivityStack.screenManager.popActivities(this@LiveIssueActivity::class.java)
                        }

                        override fun onSuccessResponseErrorCode(response: Response<String>, msg: String, msgCode: String) {
                            val intent = Intent(baseContext, LiveDoneActivity::class.java)
                            intent.putExtra("title", "发布失败")
                            intent.putExtra("message", msg)
                            startActivity(intent)
                        }

                    })
        })

        live_list.apply {
            layoutManager = FullyGridLayoutManager(baseContext, 3)
            adapter = SlimAdapter.create()
                    .register<LocalMedia>(R.layout.item_img_grid) { data, injector ->
                        injector.with<ImageView>(R.id.item_img) {
                            it.setImageURL(data.compressPath, R.mipmap.ass_camera_icon01)
                        }
                                .clicked(R.id.item_img) {
                                    PictureSelector.create(this@LiveIssueActivity)
                                            .openGallery(PictureMimeType.ofImage())
                                            .theme(R.style.picture_customer_style)
                                            .maxSelectNum(3)
                                            .minSelectNum(1)
                                            .imageSpanCount(4)
                                            .selectionMode(PictureConfig.MULTIPLE)
                                            .previewImage(true)
                                            .previewVideo(false)
                                            .isCamera(true)
                                            .imageFormat(PictureMimeType.PNG)
                                            .isZoomAnim(true)
                                            .setOutputCameraPath(Const.SAVE_FILE)
                                            .compress(true)
                                            .glideOverride(160, 160)
                                            .enableCrop(false)
                                            .withAspectRatio(4, 3)
                                            .hideBottomControls(true)
                                            .compressSavePath(cacheDir.absolutePath)
                                            .freeStyleCropEnabled(false)
                                            .circleDimmedLayer(false)
                                            .showCropFrame(true)
                                            .showCropGrid(true)
                                            .isGif(false)
                                            .openClickSound(false)
                                            .selectionMedia(selectList.filter { it.compressPath.isNotEmpty() })
                                            .previewEggs(true)
                                            .minimumCompressSize(100)
                                            .isDragFrame(false)
                                            .forResult(PictureConfig.CHOOSE_REQUEST)
                                }
                    }
                    .attachTo(this)
        }
    }

    private fun startLocation() {
        AMapLocationHelper.getInstance(baseContext).clearCodes()
        AMapLocationHelper.getInstance(baseContext).startLocation(3) { location, isSuccessed, codes ->
            if (isSuccessed && 3 in codes) {
                locationLat = location.latitude.toString()
                locationLng = location.longitude.toString()

                window.decorView.post {
                    geocoderSearch.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(
                            location.latitude, location.longitude),
                            100f, GeocodeSearch.AMAP))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PictureConfig.CHOOSE_REQUEST -> {
                    // 图片选择结果回调
                    selectList = PictureSelector.obtainMultipleResult(data) as ArrayList<LocalMedia>
                    // LocalMedia 里面返回三种path
                    // 1.media.getPath(); 为原图path
                    // 2.media.getCutPath();为裁剪后path，需判断media.isCut();是否为true
                    // 3.media.getCompressPath();为压缩后path，需判断media.isCompressed();是否为true
                    // 如果裁剪并压缩了，已取压缩路径为准，因为是先裁剪后压缩的

                    compress()
                }
            }
        }
    }

    private fun compress() {
        Flowable.just(selectList)
                .map {
                    return@map Luban.with(baseContext)
                            .setTargetDir(cacheDir.absolutePath)
                            .ignoreBy(400)
                            .loadLocalMedia(it)
                            .get()
                }
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { showLoadingDialog() }
                .doFinally { cancelLoadingDialog() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list ->
                    (0 until list.size).forEach { selectList[it].compressPath = list[it].absolutePath }
                    if (selectList.size < 3) selectList.add(LocalMedia().apply { compressPath = "" })
                    (live_list.adapter as SlimAdapter).updateData(selectList)
                }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (et_title.text.isNotBlank()
                && et_content.text.isNotBlank()) {
            bt_issue.setBackgroundResource(R.drawable.rec_bg_blue_shade)
            bt_issue.isClickable = true
        } else {
            bt_issue.setBackgroundResource(R.drawable.rec_bg_d0d0d0)
            bt_issue.isClickable = false
        }
    }
}
