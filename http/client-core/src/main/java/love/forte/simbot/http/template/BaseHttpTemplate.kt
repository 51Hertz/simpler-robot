/*
 *
 *  * Copyright (c) 2020. ForteScarlet All rights reserved.
 *  * Project  simple-robot
 *  * File     MiraiAvatar.kt
 *  *
 *  * You can contact the author through the following channels:
 *  * github https://github.com/ForteScarlet
 *  * gitee  https://gitee.com/ForteScarlet
 *  * email  ForteScarlet@163.com
 *  * QQ     1149159218
 *
 */

package love.forte.simbot.http.template


/**
 * [HttpTemplate] 基础抽象类。将部分重载方法的null参数指向最终的基本函数。
 * @author ForteScarlet
 */
@Suppress("unused")
public abstract class BaseHttpTemplate : HttpTemplate {
    /**
     * get请求。
     * @param responseType 响应body封装类型。如果为null则认为忽略返回值，则response中的getBody也为null。
     */
    override fun <T> get(url: String, responseType: Class<T>): HttpResponse<T> =
        get(url, headers = null, cookies = null, requestParam = null, responseType)

    /**
     * get请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     */
    override fun <T> get(url: String, headers: HttpHeaders?, responseType: Class<T>): HttpResponse<T> =
        get(url, headers = headers, cookies = null, requestParam = null, responseType)

    /**
     * get请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     * @param requestParam 请求参数。
     */
    override fun <T> get(url: String, cookies: HttpCookies?, responseType: Class<T>): HttpResponse<T> =
        get(url, headers = null, cookies = cookies, requestParam = null, responseType)

    /**
     * get请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     * @param requestParam 请求参数。
     */
    override fun <T> get(url: String, requestParam: Map<String, Any?>?, responseType: Class<T>): HttpResponse<T> =
        get(url, headers = null, cookies = null, requestParam = requestParam, responseType)

    /**
     * get请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     * @param requestParam 请求参数。
     */
    override fun <T> get(
        url: String,
        headers: HttpHeaders?,
        cookieMap: Map<String, String>?,
        requestParam: Map<String, Any?>?,
        responseType: Class<T>,
    ): HttpResponse<T> =
        get(url, headers = headers, cookies = cookieMap?.let { httpCookies(it) }, requestParam = requestParam, responseType)

    /**
     * post/json 请求。
     * @param responseType 响应body封装类型。
     */
    override fun <T> post(url: String, responseType: Class<T>): HttpResponse<T> =
        post(url = url, headers = null, requestBody = null, responseType)

    /**
     * post/json 请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     */
    override fun <T> post(url: String, headers: HttpHeaders?, responseType: Class<T>): HttpResponse<T> =
        post(url = url, headers = headers, requestBody = null, responseType)

    /**
     * post/form 请求。
     * @param responseType 响应body封装类型。
     */
    override fun <T> form(url: String, responseType: Class<T>): HttpResponse<T> =
        form(url = url, headers = null, requestForm = null, responseType)

    /**
     * post/form 请求。
     * @param responseType 响应body封装类型。
     * @param headers 请求头信息。
     */
    override fun <T> form(url: String, headers: HttpHeaders?, responseType: Class<T>): HttpResponse<T> =
        form(url = url, headers = headers, requestForm = null, responseType)
}