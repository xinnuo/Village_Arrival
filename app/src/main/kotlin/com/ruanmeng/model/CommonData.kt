/**
 * created by 小卷毛, 2018/3/7 0007
 * Copyright (c) 2018, 416143467@qq.com All Rights Reserved.
 * #                   *********                            #
 * #                  ************                          #
 * #                  *************                         #
 * #                 **  ***********                        #
 * #                ***  ****** *****                       #
 * #                *** *******   ****                      #
 * #               ***  ********** ****                     #
 * #              ****  *********** ****                    #
 * #            *****   ***********  *****                  #
 * #           ******   *** ********   *****                #
 * #           *****   ***   ********   ******              #
 * #          ******   ***  ***********   ******            #
 * #         ******   **** **************  ******           #
 * #        *******  ********************* *******          #
 * #        *******  ******************************         #
 * #       *******  ****** ***************** *******        #
 * #       *******  ****** ****** *********   ******        #
 * #       *******    **  ******   ******     ******        #
 * #       *******        ******    *****     *****         #
 * #        ******        *****     *****     ****          #
 * #         *****        ****      *****     ***           #
 * #          *****       ***        ***      *             #
 * #            **       ****        ****                   #
 */
package com.ruanmeng.model

import java.io.Serializable

/**
 * 项目名称：Village_Arrival
 * 创建人：小卷毛
 * 创建时间：2018-04-11 17:53
 */
data class CommonData(
        //地址列表
        var commonAddressId: String = "",
        var address: String = "",
        var detailAdress: String = "",
        var name: String = "",
        var mobile: String = "",
        var lat: String = "",
        var lng: String = "",
        //发布列表
        var buyAddress: String = "",
        var buyDetailAdress: String = "",
        var buyLat: String = "",
        var buyLng: String = "",
        var buyProvince: String = "",
        var buyCity: String = "",
        var buyDistrict: String = "",
        var buyTownship: String = "",
        var buyname: String = "",
        var buyMobile: String = "",
        var receiptAddress: String = "",
        var receiptDetailAdress: String = "",
        var receiptLat: String = "",
        var receiptLng: String = "",
        var receiptProvince: String = "",
        var receiptCity: String = "",
        var receiptDistrict: String = "",
        var receiptTownship: String = "",
        var receiptName: String = "",
        var receiptMobile: String = "",
        var receiptTime: String = "",
        var goodsOrderId: String = "",
        var goods: String = "",
        var type: String = "",
        var status: String = "",
        var goodsPrice: String = "",
        var commission: String = "",
        var tip: String = "",
        var inspection: String = "",
        var mome: String = "",
        var createDate: String = "",
        var payTime: String = "",
        var grabsingleTime: String = "",
        var arriveTime: String = "",
        var cancelTime: String = "",
        var sendUserHead: String = "",
        var sendNickName: String = "",
        var sendTelephone: String = "",
        var sendUserInfoId: String = "",
        var userGrade: String = "",
        var unAgreeCancel: String = "",
        var agreeCancel: String = "",
        var cancelType: String = "",
        //货物类型
        var goodstypeId: String = "",
        //商品重量
        var weightPriceId: String = "",
        var weightDescribe: String = "",
        //优惠券
        var endTime: String = "",
        var memo: String = "",
        var surplusCtn: String = "",
        var voucherCondition: String = "",
        var voucherCtn: String = "",
        var voucherExplain: String = "",
        var voucherId: String = "",
        var userVoucherId: String = "",
        var voucherType: String = "",
        var voucherSum: String = "",
        //互助列表
        var province: String = "",
        var city: String = "",
        var district: String = "",
        var township: String = "",
        var cooperationId: String = "",
        var title: String = "",
        var commentCtn: String = "",
        var readCtn: String = "",
        var imgs: String = "",
        //模块列表
        var appmoduleId: String = "",
        var moduleType: String = "",
        var moduleName: String = "",
        var moduleIcon: String = "",
        var value: String = "",
        //公告促销列表
        var newsType: String = "",
        var newsnoticeId: String = "",
        //轮播图列表
        var sliderId: String = "",
        var sliderImg: String = "",
        var href: String = "",
        //活动
        var activityImg: String = "",
        var activityUrl: String = "",
        var activityTitle: String = "",
        //评论列表
        var commentId: String = "",
        var content: String = "",
        var nickName: String = "",
        var userInfoId: String = "",
        var userhead: String = "",
        //交易列表
        var balance: String = "",
        var businessId: String = "",
        var tradeExplain: String = "",
        var tradeId: String = "",
        var tradeSum: String = "",
        var tradeType: String = "",
        //消息列表
        var msgId: String = "",
        var msgType: String = "",
        var sendDate: String = "",
        //常用电话
        var commonExplain: String = "",
        var commonphone: String = "",
        //乡镇列表
        var isChecked: Boolean = false,
        var areaCode: String = "",
        var areaId: String = "",
        var areaName: String = ""
) : Serializable