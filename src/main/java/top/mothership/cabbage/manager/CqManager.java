package top.mothership.cabbage.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.coolq.CqHttpApiDataResponse;
import top.mothership.cabbage.pojo.coolq.CqHttpApiGenericResponse;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.coolq.DogGroupMember;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 将CQ的HTTP API封装为接口，并托管到Spring
 *
 * @author QHS
 */
@Component
public class CqManager {
    private final String baseURL = "http://localhost:5700";

    /**
     * Send msg cq http api generic response.
     *
     * @param cqMsg the cq msg
     * @return the cq http api generic response
     */
    public CqHttpApiGenericResponse sendMsg(CqMsg cqMsg) {
        String URL;
        switch (cqMsg.getMessageType()) {
            case "group":
                URL = baseURL + "/send_group_msg";
                break;
            case "discuss":
                URL = baseURL + "/send_discuss_msg";
                break;
            case "private":
                URL = baseURL + "/send_private_msg";
                break;
            case "smoke":
                URL = baseURL + "/set_group_ban";
                break;
            case "smokeAll":
                URL = baseURL + "/set_group_whole_ban";
                break;
            case "handleInvite":
                URL = baseURL + "/set_group_add_request";
                break;
            case "kick":
                URL = baseURL + "/set_group_kick";
                break;
            default:
                return null;
        }
        HttpURLConnection httpConnection;
        try {
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            //防止转义
            //折腾了半天最后是少了UTF-8………………我tm想给自己一巴掌
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //这里不用用到下划线转驼峰
            return new Gson().fromJson(tmp2.toString(), CqHttpApiGenericResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Gets group members.
     *
     * @param groupId the group id
     * @return the group members
     */
    public CqHttpApiGenericResponse<List<DogGroupMember>> getGroupMembers(Long groupId) {
        String URL = baseURL + "/get_group_member_list";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()),"UTF-8"));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //采用泛型封装，接住变化无穷的data
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqHttpApiGenericResponse<List<DogGroupMember>>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Gets groups.
     *
     * @return the groups
     */
    public CqHttpApiGenericResponse<List<CqHttpApiDataResponse>> getGroups() {
        String URL = baseURL + "/get_group_list";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()),"UTF-8"));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqHttpApiGenericResponse<List<CqHttpApiDataResponse>>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 获取狗管理
     *
     * @param groupId 群号
     * @return 狗管理列表
     */
    public List<Long> getGroupAdmins(Long groupId) {
        List<DogGroupMember> members = getGroupMembers(groupId).getData();
        List<Long> result = new ArrayList<>();
        if(members!=null) {
            for (DogGroupMember member : members) {
                if ("admin".equals(member.getRole())) {
                    result.add(member.getQQ());
                }
            }
        }
        return result;
    }

    /**
     * 获取狗群主
     *
     * @param groupId 群号
     * @return 狗群主的QQ
     */
    public Long getOwner(Long groupId) {
        List<DogGroupMember> members = getGroupMembers(groupId).getData();
        for (DogGroupMember member : members) {
            if ("owner".equals(member.getRole())) {
                return member.getQQ();
            }
        }
        return 0L;
    }

    /**
     * 获取狗群员信息
     *
     * @param groupId 群号
     * @param userId  QQ
     * @return 群员信息
     */
    public CqHttpApiGenericResponse<DogGroupMember> getGroupMember(Long groupId, Long userId) {
        String URL = baseURL + "/get_group_member_info";
        HttpURLConnection httpConnection;
        try {
            CqMsg cqMsg = new CqMsg();
            cqMsg.setGroupId(groupId);
            cqMsg.setQQ(userId);
            httpConnection =
                    (HttpURLConnection) new URL(URL).openConnection();
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Accept", "application/json");
            httpConnection.setRequestProperty("Content-Type", "application/json");
            httpConnection.setDoOutput(true);

            OutputStream os = httpConnection.getOutputStream();
            os.write(new GsonBuilder().disableHtmlEscaping().create().toJson(cqMsg).getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader responseBuffer =
                    new BufferedReader(new InputStreamReader((httpConnection.getInputStream()),"UTF-8"));
            StringBuilder tmp2 = new StringBuilder();
            String tmp;
            while ((tmp = responseBuffer.readLine()) != null) {
                tmp2.append(tmp);
            }
            //采用泛型封装，接住变化无穷的data
            return new Gson().fromJson(tmp2.toString(), new TypeToken<CqHttpApiGenericResponse<DogGroupMember>>() {
            }.getType());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
