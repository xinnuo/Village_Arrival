package com.ruanmeng.village_arrival

import android.os.Bundle
import android.view.View
import com.lzg.extend.BaseResponse
import com.lzg.extend.jackson.JacksonDialogCallback
import com.lzy.okgo.OkGo
import com.lzy.okgo.model.Response
import com.ruanmeng.base.BaseActivity
import com.ruanmeng.base.addItems
import com.ruanmeng.base.getString
import com.ruanmeng.base.load_Linear
import com.ruanmeng.model.CommonData
import com.ruanmeng.model.LocationMessageEvent
import com.ruanmeng.share.BaseHttp
import com.ruanmeng.utils.ActivityStack
import kotlinx.android.synthetic.main.activity_task_coupon.*
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import java.util.ArrayList

class TaskCouponActivity : BaseActivity() {

    private lateinit var list: ArrayList<CommonData>
    private var isAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_coupon)
        isAccount = intent.getBooleanExtra("isAccount", false)
        init_title(if (isAccount) "我的优惠券" else "优惠券")

        if (isAccount) {
            list = ArrayList()
            getData()
        }
        else {
            @Suppress("UNCHECKED_CAST")
            list = intent.getSerializableExtra("list") as ArrayList<CommonData>
            mAdapter.updateData(list)
        }
    }

    override fun init_title() {
        super.init_title()
        coupon_list.load_Linear(baseContext)

        mAdapter = SlimAdapter.create()
                .register<CommonData>(R.layout.item_coupon_list) { data, injector ->
                    injector.text(R.id.item_coupon_money, "￥${data.voucherSum}")
                            .text(R.id.item_coupon_time, "有效期至：${data.endTime}")
                            .visibility(R.id.item_coupon_divider, if (list.indexOf(data) != 0) View.GONE else View.VISIBLE)

                            .clicked(R.id.item_coupon) {
                                if (isAccount) return@clicked

                                EventBus.getDefault().post(LocationMessageEvent("优惠券", data.userVoucherId, data.voucherSum))
                                ActivityStack.screenManager.popActivities(this@TaskCouponActivity::class.java)
                            }
                }
                .attachTo(coupon_list)
    }

    override fun getData() {
        OkGo.post<BaseResponse<ArrayList<CommonData>>>(BaseHttp.user_voucer_list)
                .tag(this@TaskCouponActivity)
                .headers("token", getString("token"))
                .execute(object : JacksonDialogCallback<BaseResponse<ArrayList<CommonData>>>(baseContext, true) {

                    override fun onSuccess(response: Response<BaseResponse<ArrayList<CommonData>>>) {
                        list.addItems(response.body().`object`)

                        mAdapter.updateData(list)
                    }

                })
    }
}
