package com.ruanmeng.village_arrival

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.View
import com.lzg.extend.BaseResponse
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.ruanmeng.base.*
import com.ruanmeng.model.CommonData
import com.ruanmeng.model.CommonModel
import com.ruanmeng.model.RefreshMessageEvent
import com.ruanmeng.share.BaseHttp
import com.ruanmeng.utils.DialogHelper
import com.ruanmeng.utils.TimeHelper
import com.ruanmeng.view.DropPopWindow
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.layout_empty.*
import kotlinx.android.synthetic.main.layout_list.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class LiveActivity : BaseActivity() {

    private val list = ArrayList<Any>()
    private var mAddress = ""
    private var mStartDate = ""
    private var mEndDate = ""

    private val listArea = ArrayList<CommonData>()
    private var dropPopWindowArea: DropPopWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)
        init_title("生活互助")

        EventBus.getDefault().register(this@LiveActivity)

        swipe_refresh.isRefreshing = true
        getData(pageNum)
    }

    override fun init_title() {
        super.init_title()
        empty_hint.text = "暂无相关生活互助信息"

        swipe_refresh.refresh { getData(1) }
        recycle_list.load_Linear(baseContext, swipe_refresh) {
            if (!isLoadingMore) {
                isLoadingMore = true
                getData(pageNum)
            }
        }

        mAdapter = SlimAdapter.create()
                .register<CommonData>(R.layout.item_live_list) { data, injector ->
                    injector.text(R.id.item_live_title, data.title)
                            .text(R.id.item_live_area, data.district)
                            .text(R.id.item_live_comment, data.commentCtn)
                            .text(R.id.item_live_time,
                                    TimeHelper.getDiffTime(TimeHelper.getInstance().millisecondToLong(data.createDate)))
                            .gone(R.id.item_live_del)
                            .gone(R.id.item_live_cancel)
                            .visibility(R.id.item_live_divider1, if (list.indexOf(data) == list.size - 1) View.GONE else View.VISIBLE)
                            .visibility(R.id.item_live_divider2, if (list.indexOf(data) != list.size - 1) View.GONE else View.VISIBLE)
                            .visibility(R.id.item_live_divider4, if (list.indexOf(data) != list.size - 1) View.GONE else View.VISIBLE)
                            .visibility(R.id.item_live_divider3, if (list.indexOf(data) != 0) View.GONE else View.VISIBLE)

                            .clicked(R.id.item_live) {
                                val intent = Intent(baseContext, LiveDetailActivity::class.java)
                                intent.putExtra("cooperationId", data.cooperationId)
                                startActivity(intent)
                            }
                }
                .attachTo(recycle_list)
    }

    override fun getData(pindex: Int) {
        OkGo.post<CommonModel>(BaseHttp.cooperation_list)
                .tag(this@LiveActivity)
                .isMultipart(true)
                .headers("token", getString("token"))
                .params("city", intent.getStringExtra("city"))
                .params("district", intent.getStringExtra("district"))
                .params("address", mAddress)
                .params("sartDate", mStartDate)
                .params("endDate", mEndDate)
                .params("type", intent.getStringExtra("type"))
                .params("page", pindex)
                .execute(object : JacksonDialogCallback<CommonModel>(baseContext, CommonModel::class.java) {

                    override fun onSuccess(response: Response<CommonModel>) {

                        list.apply {
                            if (pindex == 1) {
                                clear()
                                pageNum = pindex
                            }
                            addItems(response.body().cooperationList)
                            if (count(response.body().cooperationList) > 0) pageNum++
                        }

                        mAdapter.updateData(list)
                    }

                    override fun onFinish() {
                        super.onFinish()
                        swipe_refresh.isRefreshing = false
                        isLoadingMore = false

                        empty_view.apply { if (list.isEmpty()) visible() else gone() }
                    }

                })
    }

    override fun doClick(v: View) {
        super.doClick(v)
        when (v.id) {
            R.id.live_qu_ll -> {
                if (dropPopWindowArea == null || listArea.isEmpty()) {
                    OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.area_street2)
                            .tag(this@LiveActivity)
                            .isMultipart(true)
                            .params("areaName", intent.getStringExtra("district"))
                            .execute(object : JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(baseContext, true) {

                                @Suppress("DEPRECATION")
                                override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {
                                    listArea.apply {
                                        clear()
                                        addItems(response.body().`object`)
                                    }

                                    if (listArea.isNotEmpty()) showDropArea()
                                }

                            })
                } else {
                    if (dropPopWindowArea!!.isShowing) dropPopWindowArea!!.dismiss()
                    else showDropArea()
                }
            }
            R.id.live_time_ll -> {
                DialogHelper.showDropWindow(
                        baseContext,
                        R.layout.pop_layout_area,
                        live_time_arrow,
                        live_divider,
                        listOf("最近7天", "最近30天", "最近60天", "最近90天")) { position, name ->

                    mEndDate = TimeHelper.getInstance().stringDate

                    when (position) {
                        0 -> mStartDate = TimeHelper.getInstance().getNextDay(mEndDate, -8, "yyyy-MM-dd HH:mm:ss")
                        1 -> mStartDate = TimeHelper.getInstance().getNextDay(mEndDate, -31, "yyyy-MM-dd HH:mm:ss")
                        2 -> mStartDate = TimeHelper.getInstance().getNextDay(mEndDate, -61, "yyyy-MM-dd HH:mm:ss")
                        3 -> mStartDate = TimeHelper.getInstance().getNextDay(mEndDate, -91, "yyyy-MM-dd HH:mm:ss")
                    }
                    live_time.text = name
                    window.decorView.postDelayed({ runOnUiThread { updateList() } }, 350)
                }
            }
            R.id.bt_issue -> {
                intent.setClass(baseContext, LiveIssueActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showDropArea() {
        dropPopWindowArea = object : DropPopWindow(
                baseContext,
                R.layout.pop_layout_area,
                live_qu_arrow) {

            override fun afterInitView(view: View) {
                val recyclerView = view.findViewById<RecyclerView>(R.id.pop_container)
                recyclerView.apply {
                    load_Linear(baseContext)
                    adapter = SlimAdapter.create()
                            .register<CommonData>(R.layout.item_area_list) { data, injector ->
                                @Suppress("DEPRECATION")
                                injector.text(R.id.item_area, data.areaName)
                                        .textColor(R.id.item_area,
                                                resources.getColor(if (data.isChecked) R.color.colorAccent else R.color.black_dark))

                                        .visibility(R.id.item_area_divider1,
                                                if (listArea.indexOf(data) == listArea.size - 1) View.GONE else View.VISIBLE)
                                        .visibility(R.id.item_area_divider2,
                                                if (listArea.indexOf(data) != listArea.size - 1) View.GONE else View.VISIBLE)

                                        .clicked(R.id.item_area) {
                                            listArea.filter { it.isChecked }.forEach { it.isChecked = false }
                                            data.isChecked = true
                                            live_qu.text = data.areaName

                                            if (mAddress != data.areaName) {
                                                mAddress = data.areaName
                                                (adapter as SlimAdapter).notifyDataSetChanged()
                                                window.decorView.postDelayed({ runOnUiThread { updateList() } }, 350)
                                            }

                                            dismiss()
                                        }
                            }
                            .attachTo(this)

                    (adapter as SlimAdapter).updateData(listArea)
                }

            }
        }
        dropPopWindowArea!!.showAsDropDown(live_divider)
    }

    fun updateList() {
        swipe_refresh.isRefreshing = true

        empty_view.visibility = View.GONE
        if (list.isNotEmpty()) {
            list.clear()
            mAdapter.notifyDataSetChanged()
        }

        pageNum = 1
        getData(pageNum)
    }

    override fun finish() {
        EventBus.getDefault().unregister(this@LiveActivity)
        super.finish()
    }

    @Subscribe
    fun onMessageEvent(event: RefreshMessageEvent) {
        when (event.type) {
            "发布需求", "添加评论" -> updateList()
        }
    }
}
