package com.weqia.utils.http.okgo.request;

import android.text.TextUtils;

import com.weqia.data.UtilData;
import com.weqia.utils.http.okgo.OkGo;
import com.weqia.utils.http.okgo.SSLUtils;
import com.weqia.utils.http.okgo.adapter.CacheCall;
import com.weqia.utils.http.okgo.adapter.Call;
import com.weqia.utils.http.okgo.adapter.CallAdapter;
import com.weqia.utils.http.okgo.adapter.DefaultCallAdapter;
import com.weqia.utils.http.okgo.cache.CacheEntity;
import com.weqia.utils.http.okgo.cache.CacheMode;
import com.weqia.utils.http.okgo.callback.RequestCallBack;
import com.weqia.utils.http.okgo.convert.Converter;
import com.weqia.utils.http.okgo.model.HttpHeaders;
import com.weqia.utils.http.okgo.model.RequestParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class BaseRequest<R extends BaseRequest> extends UtilData {

    protected String url;
    protected String baseUrl;
    protected Object tag;
    protected long readTimeOut;
    protected long writeTimeOut;
    protected long connectTimeout;
    protected int retryCount;
    protected CacheMode cacheMode;
    protected String cacheKey;
    protected long cacheTime = CacheEntity.CACHE_NEVER_EXPIRE;      //默认缓存的超时时间
    private SSLUtils.SSLParams sslParams;
    protected HostnameVerifier hostnameVerifier;
    protected RequestParams params = new RequestParams();                 //添加的param
    protected HttpHeaders headers = new HttpHeaders();              //添加的header
    protected List<Interceptor> interceptors = new ArrayList<>();   //额外的拦截器
    protected List<Cookie> userCookies = new ArrayList<>();         //用户手动添加的Cookie

    private RequestCallBack mCallback;
    private Converter mConverter;
    private HttpUrl httpUrl;
    private Request mRequest;

    public BaseRequest(String url) {
        this.url = url;
        baseUrl = url;
        httpUrl = HttpUrl.parse(url);
        OkGo go = OkGo.getInstance();
        //默认添加 Accept-Language
        String acceptLanguage = HttpHeaders.getAcceptLanguage();
        if (!TextUtils.isEmpty(acceptLanguage)) headers(HttpHeaders.HEAD_KEY_ACCEPT_LANGUAGE, acceptLanguage);
        //默认添加 User-Agent
        String userAgent = HttpHeaders.getUserAgent();
        if (!TextUtils.isEmpty(userAgent)) headers(HttpHeaders.HEAD_KEY_USER_AGENT, userAgent);
        //添加公共请求参数
        if (go.getCommonParams() != null) params.put(go.getCommonParams());
        if (go.getCommonHeaders() != null) headers.put(go.getCommonHeaders());
        //添加缓存模式
        if (go.getCacheMode() != null) cacheMode = go.getCacheMode();
        //设置长连接连接关闭
        headers.put(HttpHeaders.HEAD_KEY_CONNECTION, "close");
        cacheTime = go.getCacheTime();
        //超时重试次数
        retryCount = go.getRetryCount();
    }

    @SuppressWarnings("unchecked")
    public R url(String url) {
        this.url = url;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R tag(Object tag) {
        this.tag = tag;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R readTimeOut(long readTimeOut) {
        this.readTimeOut = readTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R writeTimeOut(long writeTimeOut) {
        this.writeTimeOut = writeTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R connTimeOut(long connTimeOut) {
        this.connectTimeout = connTimeOut;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R cacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R cacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
        return (R) this;
    }

    /** 传入 -1 表示永久有效,默认值即为 -1 */
    @SuppressWarnings("unchecked")
    public R cacheTime(long cacheTime) {
        if (cacheTime <= -1) cacheTime = CacheEntity.CACHE_NEVER_EXPIRE;
        this.cacheTime = cacheTime;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCertificates(InputStream... certificates) {
        sslParams = SSLUtils.getSslSocketFactory(null, null, certificates);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCertificates(InputStream bksFile, String password, InputStream... certificates) {
        sslParams = SSLUtils.getSslSocketFactory(bksFile, password, certificates);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R headers(HttpHeaders headers) {
        this.headers.put(headers);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R headers(String key, String value) {
        headers.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeHeader(String key) {
        headers.remove(key);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeAllHeaders() {
        headers.clear();
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(RequestParams params) {
        if (params == null)
            return (R) this;
        this.params.put(params);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(Map<String, String> params) {
        this.params.put(params);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, String value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, int value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, float value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, double value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, long value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, char value) {
        params.put(key, value);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R params(String key, boolean value) {
        params.put(key, value);
        return (R) this;
    }

//    @SuppressWarnings("unchecked")
//    public R addUrlParams(String key, List<String> values) {
//    	for (String string : values) {
//			params.put
//		}
//        params.putUrlParams(key, values);
//        return (R) this;
//    }

    @SuppressWarnings("unchecked")
    public R removeParam(String key) {
        params.remove(key);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R removeAllParams() {
        params.clear();
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookie(String name, String value) {
        Cookie.Builder builder = new Cookie.Builder();
        Cookie cookie = builder.name(name).value(value).domain(httpUrl.host()).build();
        userCookies.add(cookie);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookie(Cookie cookie) {
        userCookies.add(cookie);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addCookies(List<Cookie> cookies) {
        userCookies.addAll(cookies);
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R setCallback(RequestCallBack callback) {
        this.mCallback = callback;
        return (R) this;
    }

    @SuppressWarnings("unchecked")
    public R addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        return (R) this;
    }

    /** 默认返回第一个参数 */
    public String getUrlParam(String key) {
        String value = params.urlParams.get(key);
        return value;
    }

    /** 默认返回第一个参数 */
    public RequestParams.FileWrapper getFileParam(String key) {
        RequestParams.FileWrapper value = params.fileParams.get(key);
        return value;
    }

    public RequestParams getParams() {
        return params;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Object getTag() {
        return tag;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(CacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Request getRequest() {
        return mRequest;
    }

    public RequestCallBack getCallback() {
        return mCallback;
    }

    public Converter getConverter() {
        return mConverter;
    }

    /**
     * 返回当前的请求方法
     * GET,POST,HEAD,PUT,DELETE,OPTIONS
     */
    public String getMethod() {
        return mRequest.method();
    }

    /** 根据不同的请求方式和参数，生成不同的RequestBody */
    public abstract RequestBody generateRequestBody();

    /** 对请求body进行包装，用于回调上传进度 */
    public RequestBody wrapRequestBody(RequestBody requestBody) {
        ProgressRequestBody progressRequestBody = new ProgressRequestBody(requestBody);
        progressRequestBody.setListener(new ProgressRequestBody.Listener() {
            @Override
            public void onRequestProgress(final long bytesWritten, final long contentLength, final long networkSpeed) {
                OkGo.getInstance().getDelivery().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mCallback != null) mCallback.upProgress(bytesWritten, contentLength, bytesWritten * 1.0f / contentLength, networkSpeed);
                    }
                });
            }
        });
        return progressRequestBody;
    }

    /** 根据不同的请求方式，将RequestBody转换成Request对象 */
    public abstract Request generateRequest(RequestBody requestBody);

    /** 根据当前的请求参数，生成对应的 Call 任务 */
    public okhttp3.Call generateCall(Request request) {
        mRequest = request;
        if (readTimeOut <= 0 && writeTimeOut <= 0 && connectTimeout <= 0 && sslParams == null && userCookies.size() == 0) {
            return OkGo.getInstance().getOkHttpClient().newCall(request);
        } else {
            OkHttpClient.Builder newClientBuilder = OkGo.getInstance().getOkHttpClient().newBuilder();
            if (readTimeOut > 0) newClientBuilder.readTimeout(readTimeOut, TimeUnit.MILLISECONDS);
            if (writeTimeOut > 0) newClientBuilder.writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS);
            if (connectTimeout > 0) newClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            if (hostnameVerifier != null) newClientBuilder.hostnameVerifier(hostnameVerifier);
            if (sslParams != null) newClientBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
            if (userCookies.size() > 0) OkGo.getInstance().getCookieJar().addCookies(userCookies);
            if (interceptors.size() > 0) {
                for (Interceptor interceptor : interceptors) {
                    newClientBuilder.addInterceptor(interceptor);
                }
            }
            return newClientBuilder.build().newCall(request);
        }
    }

    /** 获取同步call对象 */
    public okhttp3.Call getCall() {
        //构建请求体，返回call对象
        RequestBody requestBody = generateRequestBody();
        mRequest = generateRequest(wrapRequestBody(requestBody));
        return generateCall(mRequest);
    }

    /** Rx支持,获取同步call对象 */
    public <T> Call<T> getCall(Converter<T> converter) {
        mConverter = converter;
        return DefaultCallAdapter.<T>create().adapt(new CacheCall<T>(this));
    }

    /** Rx支持,获取同步call对象 */
    public <T, E> E getCall(Converter<T> converter, CallAdapter<E> adapter) {
        mConverter = converter;
        return adapter.adapt(getCall(converter));
    }

    /** 普通调用，阻塞方法，同步请求执行 */
    public Response execute() throws IOException {
        return getCall().execute();
    }

    /** 非阻塞方法，异步请求，但是回调在子线程中执行 */
    @SuppressWarnings("unchecked")
    public <T> void execute(RequestCallBack<T> callback) {
        mCallback = callback;
        mConverter = callback;
        new CacheCall<T>(this).execute(callback);
    }
}