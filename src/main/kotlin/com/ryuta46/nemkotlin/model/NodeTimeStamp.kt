//
//  Created by Taizo Kusuda on 2018/12/26.
//  Copyright © 2018 T TECH, LIMITED LIABILITY CO. All rights reserved.
//
package com.ryuta46.nemkotlin.model

data class NodeTimeStamp(
        val sendTimeStamp: Long,
        val receiveTimeStamp: Long
) {
    val receiveTimeStampBySeconds: Int get()  {
        return (receiveTimeStamp / 1000).toInt()
    }
}
