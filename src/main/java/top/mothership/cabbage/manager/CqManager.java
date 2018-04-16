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
    public List<Long> getGroupAdmins(Long groupId) {
        List<DogGroupMember> members = getGroupMembers(groupId).getData();
        List<Long> result = new ArrayList<>();
        if(members!=null) {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getRole().equals("admin")) {
                    result.add(members.get(i).getUserId());
                }
            }
        }
        return result;
    }

    public Long getOwner(Long groupId) {
        List<DogGroupMember> members = getGroupMembers(groupId).getData();
        for(int i=0;i<members.size();i++){
            if(members.get(i).getRole().equals("owner")){
                return members.get(i).getUserId();
            }
        }
        return 0L;
    }

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
