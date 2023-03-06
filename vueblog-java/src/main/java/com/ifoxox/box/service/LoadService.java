package com.ifoxox.box.service;

import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ifoxox.box.bean.LiveChannelGroup;
import com.ifoxox.box.bean.ParseBean;
import com.ifoxox.box.bean.SourceBean;
import com.ifoxox.box.common.AppKeys;
import com.ifoxox.box.config.DefaultConfig;
import com.ifoxox.box.util.OkHttpUtil;
import com.ifoxox.box.util.RemoteServerUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author lsj
 */
@Service
public class LoadService {


    private final StringRedisTemplate redisTemplate;

    private final OkHttpUtil okHttpUtil;

    public LoadService(StringRedisTemplate redisTemplate, OkHttpUtil okHttpUtil) {
        this.redisTemplate = redisTemplate;
        this.okHttpUtil = okHttpUtil;
    }

    public void loadUrl(String apiUrl) {
//        redisTemplate.opsForValue().get(AppKeys.API_URL)
        String s = okHttpUtil.doGet(apiUrl);

        System.out.println(s);
        parseJson(apiUrl, s);
    }

    private void parseJson(String apiUrl, String jsonStr) {
        JsonObject infoJson = new Gson().fromJson(jsonStr, JsonObject.class);
        ValueOperations<String, String> stringValueOperations = redisTemplate.opsForValue();
        LinkedHashMap<String, SourceBean> sourceBeanList = new LinkedHashMap<>();

        // spider
        String spider = DefaultConfig.safeJsonString(infoJson, "spider", "");
        stringValueOperations.set(AppKeys.API_SPIDER,spider);
        // 远端站点源
        SourceBean firstSite = null;
        for (JsonElement opt : infoJson.get("sites").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            SourceBean sb = new SourceBean();
            String siteKey = obj.get("key").getAsString().trim();
            sb.setKey(siteKey);
            sb.setName(obj.get("name").getAsString().trim());
            sb.setType(obj.get("type").getAsInt());
            sb.setApi(obj.get("api").getAsString().trim());
            sb.setSearchable(DefaultConfig.safeJsonInt(obj, "searchable", 1));
            sb.setQuickSearch(DefaultConfig.safeJsonInt(obj, "quickSearch", 1));
            sb.setFilterable(DefaultConfig.safeJsonInt(obj, "filterable", 1));
            sb.setPlayerUrl(DefaultConfig.safeJsonString(obj, "playUrl", ""));
            sb.setExt(DefaultConfig.safeJsonString(obj, "ext", ""));
            sb.setCategories(DefaultConfig.safeJsonStringList(obj, "categories"));
            if (firstSite == null) {
                firstSite = sb;
            }
            sourceBeanList.put(siteKey, sb);
        }
        if (sourceBeanList != null && sourceBeanList.size() > 0) {
            String home = stringValueOperations.get(AppKeys.HOME_API);
            SourceBean sh = getSource(home,sourceBeanList);
            if (sh == null) {
                stringValueOperations.set(AppKeys.HOME_API, firstSite.getKey());
            } else {
                stringValueOperations.set(AppKeys.HOME_API, sh.getKey());
            }
        }
        stringValueOperations.set(AppKeys.SOURCE_BEAN_LIST, JSONObject.toJSONString(sourceBeanList));


        // 需要使用vip解析的flag
        List<String> vipParseFlags = DefaultConfig.safeJsonStringList(infoJson, "flags");
        stringValueOperations.set(AppKeys.VIP_PARSE_FLAGS, JSONObject.toJSONString(vipParseFlags));

        List<ParseBean> parseBeanList = new ArrayList<>();
        // 解析地址
        for (JsonElement opt : infoJson.get("parses").getAsJsonArray()) {
            JsonObject obj = (JsonObject) opt;
            ParseBean pb = new ParseBean();
            pb.setName(obj.get("name").getAsString().trim());
            pb.setUrl(obj.get("url").getAsString().trim());
            String ext = obj.has("ext") ? obj.get("ext").getAsJsonObject().toString() : "";
            pb.setExt(ext);
            pb.setType(DefaultConfig.safeJsonInt(obj, "type", 0));
            parseBeanList.add(pb);
        }
        stringValueOperations.set(AppKeys.PARSE_BEAN_LIST, JSONObject.toJSONString(parseBeanList));
        // 获取默认解析
        if (parseBeanList != null && parseBeanList.size() > 0) {
            String defaultParse = stringValueOperations.get(AppKeys.DEFAULT_PARSE);
            if (!StringUtils.isEmpty(defaultParse)) {
                for (ParseBean pb : parseBeanList) {
                    if (pb.getName().equals(defaultParse)) {
                        pb.setDefault(true);
                        stringValueOperations.set(AppKeys.DEFAULT_PARSE, pb.getName());
                    }
                }
            }
            if (stringValueOperations.get(AppKeys.DEFAULT_PARSE) == null) {
                parseBeanList.get(0).setDefault(true);
                stringValueOperations.set(AppKeys.DEFAULT_PARSE, parseBeanList.get(0).getName());
            }

        }
//        List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
//        // 直播源
//        liveChannelGroupList.clear();
//        stringValueOperations.set(AppKeys.DEFAULT_PARSE, parseBeanList.get(0).getName());//修复从后台切换重复加载频道列表
//        try {
//            String lives = infoJson.get("lives").getAsJsonArray().toString();
//            int index = lives.indexOf("proxy://");
//            if (index != -1) {
//                int endIndex = lives.lastIndexOf("\"");
//                String url = lives.substring(index, endIndex);
//                url = DefaultConfig.checkReplaceProxy(url);
//
//                //clan
//                String extUrl = URI.parse(url).getQueryParameter("ext");
//                if (extUrl != null && !extUrl.isEmpty()) {
//                    String extUrlFix = new String(Base64.decode(extUrl, Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
//                    if (extUrlFix.startsWith("clan://")) {
//                        extUrlFix = clanContentFix(clanToAddress(apiUrl), extUrlFix);
//                        extUrlFix = Base64.encodeToString(extUrlFix.getBytes("UTF-8"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP);
//                        url = url.replace(extUrl, extUrlFix);
//                    }
//                }
//                LiveChannelGroup liveChannelGroup = new LiveChannelGroup();
//                liveChannelGroup.setGroupName(url);
//                liveChannelGroupList.add(liveChannelGroup);
//            } else {
//                loadLives(infoJson.get("lives").getAsJsonArray());
//            }
//        } catch (Throwable th) {
//            th.printStackTrace();
//        }
    }


    public SourceBean getSource(String key, LinkedHashMap<String, SourceBean> sourceBeanList) {
        if (!sourceBeanList.containsKey(key)) {
            return null;
        }
        return sourceBeanList.get(key);
    }

    public String clanToAddress(String lanLink) {
        if (lanLink.startsWith("clan://localhost/")) {
            return lanLink.replace("clan://localhost/", RemoteServerUtil.getAddress(true) + "file/");
        } else {
            String link = lanLink.substring(7);
            int end = link.indexOf('/');
            return "http://" + link.substring(0, end) + "/file/" + link.substring(end + 1);
        }
    }

    public String clanContentFix(String lanLink, String content) {
        String fix = lanLink.substring(0, lanLink.indexOf("/file/") + 6);
        return content.replace("clan://", fix);
    }
}
