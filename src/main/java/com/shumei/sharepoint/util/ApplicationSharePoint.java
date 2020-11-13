package com.shumei.sharepoint.util;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.auth.confidentialClient.ClientCredentialProvider;
import com.microsoft.graph.auth.enums.NationalCloud;
import com.microsoft.graph.concurrency.ChunkedUploadProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.*;
import com.shumei.sharepoint.configuration.AadConfig;
import com.shumei.sharepoint.entity.SharePointFile;
import com.shumei.sharepoint.entity.SharePointFolder;
import com.shumei.sharepoint.enums.PermissionEnum;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xushuai
 * @description 应用程序操作SharePoint工具类
 * */
public class ApplicationSharePoint {
    private static final String DOWNLOAD_URL = "https://graph.microsoft.com/v1.0/groups/%s/drive/items/root:/%s:/content";
    private static final String INVITE_REDIRECT_URL = "https://myapps.microsoft.com/?tenantid=%s&login_hint=%s";
    private static final String INVITE_USER_TO_GROUP_URL = "https://graph.microsoft.com/v1.0/groups/%s/members/$ref";
    private static final String SCOPE = "https://graph.microsoft.com/.default";

    public static final String CONFLICT_BEHAVIOR_REPLACE = "replace";
    public static final String CONFLICT_BEHAVIOR_FAIL = "fail";
    public static final String CONFLICT_BEHAVIOR_RENAME = "rename";

    public static final List<String> illegalChars = Arrays.asList("*", "\\", "|", ":", "\"", "<", ">", "/", "?", ".");

    private static final HashMap<String,String> fileNameIllegalChars = new HashMap<>();

    static ClientCredentialProvider authProvider = null;
    static IGraphServiceClient graphClient = null;

    static {
        fileNameIllegalChars.put("[","%5b");
        fileNameIllegalChars.put("]","%5d");
    }

    @Autowired
    AadConfig config;

    /**
     * 创建microsoft graph对象
     */
    @PostConstruct
    public void init() {
        if (StringUtils.isNotBlank(config.getClientId()) && StringUtils.isNotBlank(config.getClientSecret()) && StringUtils.isNotBlank(config.getTenantId())) {
            NationalCloud area = config.getNationalCloud();
            authProvider = new ClientCredentialProvider(config.getClientId(),
                    Arrays.asList(SCOPE), config.getClientSecret(), config.getTenantId(), area);
            graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationSharePoint sharePoint = new ApplicationSharePoint();
        authProvider = new ClientCredentialProvider("ebf059a1-74ef-4642-bdb5-e8ad889fad28",
                Arrays.asList(SCOPE), "5610k-d~FsY.OadiFDt0elOemBm-enT6VK", "ff9553e4-9c23-450c-81e3-4891cda7d10a", NationalCloud.Global);
        graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        //sharePoint.createGroup("SSS","SSS");
        sharePoint.inviteMemberToGroup("7a3a753f-5448-4661-91c3-e80878a2cd1f","aabb12f8-b584-414b-b4a8-16cdfcf32cf2");
    }


    /**
     * @param displayName  要在组的通讯簿中显示的名称。 必需。
     * @param mailNickName 组的邮件别名。
     */
    public Group createGroup(String displayName, String mailNickName) {
        return createGroup(displayName, mailNickName, null);
    }

    /**
     * @param displayName  要在组的通讯簿中显示的名称。 必需。
     * @param mailNickName 组的邮件别名。
     * @param description  组说明。 可选
     */
    public Group createGroup(String displayName, String mailNickName, String description) {
        LinkedList<String> groupTypesList = new LinkedList<String>();
        groupTypesList.add("Unified");
        return createGroup(displayName, mailNickName, description, groupTypesList);
    }

    /**
     * @param displayName  要在组的通讯簿中显示的名称。 必需。
     * @param mailNickName 组的邮件别名。
     * @param description  组说明。 可选
     * @param groupTypes   使用 groupTypes 属性来控制组的类型及其成员身份
     * @return 返回新创建的组
     */
    public Group createGroup(String displayName, String mailNickName, String description, List<String> groupTypes) {
        Assert.hasText("displayName", "组通讯簿名称不能为空");
        Assert.hasText("mailNickName", "组邮件名称不能为空");
        Group group = new Group();
        if (StringUtils.isNotBlank(description)) {
            group.description = description;
        }
        if (groupTypes != null) {
            group.groupTypes = groupTypes;
        }
        group.displayName = displayName;
        group.mailEnabled = true;
        group.mailNickname = mailNickName;
        group.securityEnabled = false;
        group.visibility = "private";
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("WelcomeEmailDisabled");
        group.additionalDataManager().put("resourceBehaviorOptions",jsonArray);
        return graphClient.groups()
                .buildRequest()
                .post(group);
    }

    /**
     * @param groupName 根据组名查询组信息 eq查询
     * @return
     */
    public Group queryGroupByGroupName(String groupName) {
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new QueryOption("$filter", String.format("displayName eq '%s'", groupName)));
        IGroupCollectionPage page = graphClient.groups().buildRequest(requestOptions).top(1).get();
        List<Group> list = null;
        if (page != null && (list = page.getCurrentPage()) != null && page.getCurrentPage().size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * @param groupName 判断组在sharepoint上是否存在
     * @return Boolean
     */
    public boolean checkGroupExist(String groupName) {
        return queryGroupByGroupName(groupName) != null;
    }

    /**
     * @param groupId    组id
     * @param folderName 文件夹名称
     */
    public SharePointFolder createFolder(String groupId, String folderName) {
        return createFolder(groupId, folderName, CONFLICT_BEHAVIOR_FAIL);
    }

    /**
     * @param groupId          组id
     * @param folderName       文件夹名称
     * @param conflictBehavior 冲突策略 默认为rename 重命名
     */
    public SharePointFolder createFolder(String groupId, String folderName, String conflictBehavior) {
        Assert.hasText(groupId, "组id不能为空");
        Assert.hasText(folderName, "文件夹名称不能为空");
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        DriveItem driveItem = new DriveItem();
        driveItem.name = getRealFolderName(folderName);
        Folder folder = new Folder();
        driveItem.folder = folder;
        if (StringUtils.isBlank(conflictBehavior)) {
            conflictBehavior = CONFLICT_BEHAVIOR_RENAME;
        }
        driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive(conflictBehavior));
        try {
            driveItem = graphClient.groups(groupId).drive().root().children().buildRequest().post(driveItem);
        } catch (GraphServiceException ex) {
            if (ex.getResponseCode() == 409) {
                driveItem = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(driveItem.name)).buildRequest().get();
            } else {
                throw ex;
            }
        }
        SharePointFolder sharePointFolder = new SharePointFolder();
        sharePointFolder.setFolderId(driveItem.id);
        sharePointFolder.setPath(driveItem.name);
        sharePointFolder.setGroupId(groupId);
        sharePointFolder.setFolderName(driveItem.name);
        return sharePointFolder;
    }

    /**
     * @param groupId      组id
     * @param parentFolder 父目录名称;从根目录到将创建目录的路径;如果为空则将在根目录创建目录
     * @param folderName   要创建的文件名
     * @return List<Folder> 被新创建的文件夹
     * @description 创建目录, 如果parentFolder不为空且sharepoint上不存在，则先新建父目录；
     */
    public List<SharePointFolder> createFolderAndParentIfAbsent(String groupId, String parentFolder, String folderName) {
        Assert.hasText(groupId, "组id不能为空");
        Assert.hasText(folderName, "文件夹名称不能为空");
        List<SharePointFolder> newFolders = new LinkedList<>();
        StringBuilder builder = new StringBuilder();
        String path = null;
        if (StringUtils.isNotBlank(parentFolder)) {
            path = parentFolder + "/" + folderName;
        } else {
            path = folderName;
        }
        if (StringUtils.isNotBlank(path)) {
            String[] f = path.split("/");
            for (String inst : f) {
                DriveItem driveItem = new DriveItem();
                driveItem.name = inst;
                Folder folder = new Folder();
                driveItem.folder = folder;
                driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive(CONFLICT_BEHAVIOR_FAIL));
                try {
                    if (builder.length() == 0) {
                        driveItem = graphClient.groups(groupId).drive().root().children().buildRequest().post(driveItem);
                    } else {
                        driveItem = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(builder.toString())).children().buildRequest().post(driveItem);
                    }
                    SharePointFolder newSharePointFolder = new SharePointFolder();
                    newSharePointFolder.setFolderId(driveItem.id);
                    newSharePointFolder.setFolderName(driveItem.name);
                    newSharePointFolder.setGroupId(groupId);
                    newSharePointFolder.setPath(builder.toString());
                    newFolders.add(newSharePointFolder);
                } catch (GraphServiceException exception) {
                    // http code 409 conflict 说明文件夹已存在
                    if (409 == exception.getResponseCode()) {
                        continue;
                    }
                    throw exception;
                } finally {
                    builder.append("/").append(inst);
                }
            }
        }
        return newFolders;
    }

    @Deprecated
    private void uploadFile(java.io.File file) throws IOException {
        OkHttpClient client = HttpClients.createDefault(authProvider);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), file))
                .build();
        String aaa = String.format("https://graph.microsoft.com/v1.0/groups/%s/drive/items/%s:/%s:/content",
                "b046caaf-f1f0-4d06-a123-49c9bf9d3cb9", "01X2KEMBV6Y2GOVW7725BZO354PWSELRRZ", file.getName());
        Request request = new Request.Builder().url(aaa).put(requestBody).build();
        Response response = client.newCall(request).execute();
    }

    /**
     * @param groupId          组id
     * @param file             文件
     * @param conflictBehavior 冲突策略
     * @param path             上传位置
     * @description 上传文件，文件大于4M的需要使用大文件分段上传的方法
     */
    public SharePointFile uploadFile(String groupId, java.io.File file, String conflictBehavior, String path, boolean canWrite) throws IOException {
        DriveItem driveItem = new DriveItem();
        driveItem.name = file.getName();
        driveItem.file = new File();
        if (StringUtils.isBlank(conflictBehavior)) {
            conflictBehavior = CONFLICT_BEHAVIOR_RENAME;
        }
        driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive(conflictBehavior));
        if (StringUtils.isBlank(path)) {
            path = "";
            driveItem = graphClient.groups(groupId).drive().root().children().buildRequest().post(driveItem);
        } else {
            driveItem = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(path)).children().buildRequest().post(driveItem);
        }
        driveItem.name = getEncodeFileName(driveItem.name);
        try {
            SharePointFile uploadFile = null;
            if ((file.length() / (1024 * 1024)) >= 3L) {
                uploadFile = uploadLargeFile(groupId, path, file, driveItem);
            } else {
                uploadFile = uploadFile(groupId, path, file, driveItem);
            }
            if (canWrite == false) {
                checkOutItem(groupId, path + "/" + getEncodeFileName(uploadFile.getFileName()));
            }
            return uploadFile;
        } finally {
            file.delete();
        }
    }

    private SharePointFile uploadFile(String groupId, String path, java.io.File file, DriveItem driveItem) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        stream.read(bytes);
        driveItem = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(path + "/" + driveItem.name)).content().buildRequest().put(bytes);
        SharePointFile sharePointFile = new SharePointFile();
        sharePointFile.setFileId(driveItem.id);
        sharePointFile.setFileName(driveItem.name);
        sharePointFile.setViewUrl(driveItem.webUrl);
        return sharePointFile;
    }

    public SharePointFile uploadLargeFile(String groupId, String path, java.io.File file, DriveItem driveItem) throws IOException {
        InputStream fileStream = new FileInputStream(file);
        long streamSize = file.length();
        SharePointFile sharePointFile = new SharePointFile();
        IProgressCallback<DriveItem> callback = new IProgressCallback<DriveItem>() {
            @Override
            public void progress(final long current, final long max) {
                System.out.println(
                        String.format("Uploaded %d bytes of %d total bytes", current, max)
                );
            }

            @Override
            public void success(final DriveItem result) {
                sharePointFile.setFileId(result.id);
                sharePointFile.setFileName(result.name);
                sharePointFile.setViewUrl(result.webUrl);
            }

            @Override
            public void failure(final ClientException ex) {
                throw new RuntimeException("上传文件失败", ex);
            }
        };
        UploadSession uploadSession = graphClient.groups(groupId)
                .drive()
                .root()
                .itemWithPath(spaceEncode(path + "/" + driveItem.name))
                .createUploadSession(new DriveItemUploadableProperties())
                .buildRequest()
                .post();

        ChunkedUploadProvider<DriveItem> chunkedUploadProvider =
                new ChunkedUploadProvider<DriveItem>
                        (uploadSession, graphClient, fileStream, streamSize, DriveItem.class);
        // Config parameter is an array of integers
        // customConfig[0] indicates the max slice size
        // Max slice size must be a multiple of 320 KiB
        int[] customConfig = {320 * 1024};
        chunkedUploadProvider.upload(callback, customConfig);
        return sharePointFile;
    }

    /**
     * @param groupId  组id
     * @param itemName 文件或文件夹名称
     *                 删除文件或文件夹
     */
    public void deleteItem(String groupId, String itemName) {
        Assert.hasText(groupId, "groupId不能为空");
        Assert.hasText(itemName, "要删除的文件夹或文件的名称不能为空");
        graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemName))
                .buildRequest()
                .delete();
    }

    /**
     * @param groupId  组id
     * @param itemName 文件或文件夹名称
     *                 删除文件或文件夹
     */
    public void deleteItem(String groupId, String path, String itemName) {
        Assert.hasText(groupId, "groupId不能为空");
        Assert.hasText(itemName, "要删除的文件夹或文件的名称不能为空");
        try {
            graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(path + "/" + itemName))
                    .buildRequest()
                    .delete();
        } catch (GraphServiceException ex) {
            if (ex.getResponseCode() == 404) {
                return;
            }
            throw ex;
        }
    }

    /**
     * @param groupId  组id
     * @param itemName 文件名称
     *                 下载文件
     */
    public void downloadFile(String groupId, String itemName, HttpServletResponse response) throws IOException {
        Assert.hasText(groupId, "groupId不能为空");
        Assert.hasText(itemName, "要下载的文件名称不能为空");
        OkHttpClient client = HttpClients.createDefault(authProvider);
        String realUrl = String.format(DOWNLOAD_URL, groupId, itemName);
        Request request = new Request.Builder().url(realUrl).build();
        Response response1 = client.newCall(request).execute();
        response.sendRedirect(response1.request().url().toString());
    }

    public InputStream downloadFile(String groupId, String filePath) {
        return graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(filePath)).content().buildRequest().get();
    }

    /**
     * 得到应用上所有用户
     */
    public List<User> querySharePointUsers() {
        String fields = "id,displayName,givenName,mail,userPrincipalName";
        IUserCollectionPage userPage = graphClient.users().buildRequest().select(fields).get();
        List<User> users = userPage.getCurrentPage();
        while ((users != null && users.size() > 0) && userPage.getNextPage() != null) {
            userPage = userPage.getNextPage().buildRequest().select(fields).get();
            users.addAll(userPage.getCurrentPage());
        }
        return users;
    }

    /**
     * @param mail 邮箱
     *             根据用户邮箱返回应用上的用户信息
     */
    public User querySharePointUserByMail(String mail) {
        String fields = "id,displayName,mail,userPrincipalName";
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new QueryOption("$filter", "(mail eq '" + mail + "') or (userPrincipalName eq '" + mail + "')"));
        IUserCollectionPage userPage = graphClient.users().buildRequest(requestOptions).top(1).select(fields).get();
        User user = null;
        if (userPage.getCurrentPage().size() > 0) {
            user = userPage.getCurrentPage().get(0);
        }
        return user;
    }

    /**
     * @param email 邮箱；将会给参数email代表的邮箱发邀请邮件
     *              邀请用户到应用上
     */
    public void inviteUserToAAD(String email) {
        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = email;
        invitation.inviteRedirectUrl = String.format(INVITE_REDIRECT_URL, config.getTenantId(), email);
        invitation.sendInvitationMessage = true;
        graphClient.invitations()
                .buildRequest()
                .post(invitation);
    }

    /**
     * @param userId 用户在应用上的id
     *               从应用用户中删除用户
     */
    public void deleteUserFromAAD(String userId) {
        //提示权限不足 Insufficient privileges to complete the operation
        graphClient.users(userId)
                .buildRequest()
                .delete();
    }

    /**
     * @param groupId 组id
     * @param userId  用户在应用上的id
     *                将用户添加到组内
     */
    public void inviteMemberToGroup(String groupId, String userId) throws IOException {
        String url = String.format(INVITE_USER_TO_GROUP_URL, groupId);
        OkHttpClient client = HttpClients.createDefault(authProvider);
        Gson gson = new Gson();
        HashMap<String, String> hashMap = new HashMap<>(1);
        String param = "https://graph.microsoft.com/v1.0/directoryObjects/" + userId;
        hashMap.put("@odata.id", param);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(hashMap));
        Request request = new Request.Builder().addHeader("Content-type", "application/json")
                .addHeader("Content-length", String.valueOf(requestBody.contentLength())).url(url).post(requestBody).build();
        Response response1 = client.newCall(request).execute();
        if (!response1.isSuccessful()) {
            System.out.println(response1.body().string());
        }
    }

    /**
     * @param groupId 组id
     * @param userId  用户在应用上的id
     *                将用户从组内删除
     */
    public void removeMemberFromGroup(String groupId, String userId) {
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        graphClient.groups(groupId).members(userId).reference()
                .buildRequest()
                .delete();
    }

    /**
     * @param groupId 组id
     * @param userId  用户在应用上的id
     *                将用户设置为组的拥有者
     */
    public void addGroupOwner(String groupId, String userId) {
        DirectoryObject directoryObject = new DirectoryObject();
        directoryObject.id = userId;
        graphClient.groups(groupId).owners().references()
                .buildRequest()
                .post(directoryObject);
    }

    @Deprecated
    public void inviteUserToAAD(String email, String tenant) {
        Invitation invitation = new Invitation();
        invitation.invitedUserEmailAddress = email;
        invitation.inviteRedirectUrl = String.format(INVITE_REDIRECT_URL, tenant, email);
        invitation.sendInvitationMessage = true;
        graphClient.invitations()
                .buildRequest()
                .post(invitation);
    }

    /**
     * @param groupId  组id
     * @param itemPath DriveItem的路径
     *                 签出driveItem资源，以防止其他人编辑该文档
     * @see https://docs.microsoft.com/zh-cn/graph/api/driveitem-checkout?view=graph-rest-1.0&tabs=http
     */
    public void checkOutItem(String groupId, String itemPath) {
        graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).checkout()
                .buildRequest().post();
    }

    /**
     * @param groupId  组id
     * @param itemPath DriveItem的路径
     *                 签入已签出的driveItem资源，使其他用户可以使用该文档
     * @see https://docs.microsoft.com/zh-cn/graph/api/driveitem-checkin?view=graph-rest-1.0&tabs=http
     */
    public void checkInItem(String groupId, String itemPath) {
        checkInItem(groupId, itemPath, null);
    }

    /**
     * @param groupId  组id
     * @param itemPath DriveItem的路径
     * @param comment  与此版本相关联的签入注释
     * @see https://docs.microsoft.com/zh-cn/graph/api/driveitem-checkin?view=graph-rest-1.0&tabs=http
     * 签入已签出的driveItem资源，使其他用户可以使用该文档
     */
    public void checkInItem(String groupId, String itemPath, String comment) {
        graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).checkin(null, comment)
                .buildRequest().post();
    }

    /**
     * @param groupId            组id
     * @param itemPath           DriveItem的路径
     * @param type               可选 创建的共享链接的类型 view，edit
     * @param password           可选 密码
     * @param expirationDateTime 可选 该权限的过期时间
     * @param scope              可选 要创建的链接的范围。 anonymous 或 organization
     */
    private void createLink(String groupId, String itemPath, String type, String password, java.util.Calendar expirationDateTime, String scope) {
        Permission permission = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).createLink(type, scope, expirationDateTime, password, null)
                .buildRequest().post();
    }

    private void grantTo(List<String> recipitentEmails, String role) {
        LinkedList<DriveRecipient> recipientsList = new LinkedList<DriveRecipient>();
        for (String mail : recipitentEmails) {
            DriveRecipient recipients = new DriveRecipient();
            recipients.email = mail;
            recipientsList.add(recipients);
        }
        LinkedList<String> rolesList = new LinkedList<String>();
        rolesList.add(role);
        graphClient.shares("{encode-share-url}").permission()
                .grant(rolesList, recipientsList)
                .buildRequest()
                .post();
    }

    private void listPermissionOfItem(String groupId, String itemPath) {
        IPermissionCollectionPage permissions = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).permissions()
                .buildRequest()
                .get();
        System.out.println(111);
    }

    public List<DriveItem> listFile(String groupId, String itemPath) {
        IDriveItemCollectionPage driveItemCollectionPage = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).children()
                .buildRequest().top(2)
                .get();
        List<DriveItem> result = new LinkedList<>();
        List<DriveItem> listItem = driveItemCollectionPage.getCurrentPage();
        if (listItem != null) {
            result.addAll(listItem.stream().filter(a -> a.file != null).collect(Collectors.toList()));
            while (driveItemCollectionPage.getNextPage() != null) {
                driveItemCollectionPage = driveItemCollectionPage.getNextPage().buildRequest().get();
                if (driveItemCollectionPage.getCurrentPage() != null) {
                    result.addAll(driveItemCollectionPage.getCurrentPage().stream().filter(a -> a.file != null).collect(Collectors.toList()));
                }
            }
        }
        return result;
    }

    private static void sharingUrl() throws UnsupportedEncodingException {
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        LinkedList<DriveRecipient> recipientsList = new LinkedList<DriveRecipient>();
        DriveRecipient recipients = new DriveRecipient();
        recipients.email = "944945546@qq.com";
        recipientsList.add(recipients);
        LinkedList<String> rolesList = new LinkedList<String>();
        rolesList.add("read");
        graphClient.shares(encodeSharingUrl("https://xiaoyuanmiao.sharepoint.com/:f:/s/2020Q3-RMB-FUND/EqXf07JN2Q1OhOvTxB3Wy9sBrykW1Oou2e1XBpavZXpKXA")).permission()
                .grant(rolesList, recipientsList)
                .buildRequest()
                .post();
    }

    private static String encodeSharingUrl(String sharingUrl) throws UnsupportedEncodingException {
        String base64Value = Base64.getEncoder().encodeToString(sharingUrl.getBytes("utf-8"));
        return "u!" + base64Value.replace("=", "").replace('/', '_').replace('+', '-');
    }

    public String sharesItem(String groupId, String itemPath, String recipientEmail, boolean requireSignIn, boolean sendInvitation, PermissionEnum permission) {
        Assert.hasText(recipientEmail, "接收邮件不能为空");
        LinkedList<DriveRecipient> recipientsList = new LinkedList<>();
        DriveRecipient recipients = new DriveRecipient();
        recipients.email = recipientEmail;
        recipientsList.add(recipients);
        String message = "Here's the file that we're collaborating on.";
        LinkedList<String> rolesList = new LinkedList<String>();
        if (permission == PermissionEnum.READ) {
            rolesList.add("read");
        } else if (permission == PermissionEnum.WRITE) {
            rolesList.add("write");
        }
        //邀请该组织内aad用户时result的id有返回值，邀请不在aad上的用户时没有id
        IDriveItemInviteCollectionPage result = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath))
                .invite(requireSignIn, rolesList, sendInvitation, message, recipientsList, null, null)
                .buildRequest()
                .post();
        return result.getCurrentPage().get(0).id;
    }

    /**
     * @param groupId       组id
     * @param itemPath      DriveItem的路径
     * @param permissionsId 共享项目权限的Id
     *                      将用户从共享项目权限里删除
     */
    public void deleteUserFromPermissionOfItem(String groupId, String itemPath, String permissionsId) {
        graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).permissions(permissionsId)
                .buildRequest()
                .delete();
    }

    private String getRealFolderName(String itemName) {
        for (String charInst : illegalChars) {
            itemName = itemName.replace(charInst, "");
        }
        return itemName;
    }

    private String getEncodeFileName(String itemName) {
        for (Map.Entry<String,String> inst: fileNameIllegalChars.entrySet()) {
            if (itemName.contains(inst.getKey())){
                itemName = itemName.replace(inst.getKey(),inst.getValue());
            }
        }
        return itemName;
    }

    private void listGroup() {
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new QueryOption("$filter", String.format("displayName eq '%s'", "ShareDocument")));
        IGroupCollectionPage aaa = graphClient.groups().buildRequest().get();
        System.out.println(111);
    }

    private void listItem(String s) {
        IDriveItemCollectionPage aaa = graphClient.groups(s).drive().root().children().buildRequest().get();
        for (DriveItem item : aaa.getCurrentPage()) {
            System.out.println(item.name);
        }
        System.out.println(111);
    }

    private void listApplication() {
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

        IApplicationCollectionPage applications = graphClient.applications()
                .buildRequest()
                .get();
        System.out.println(111);
    }

    private void listGroupMember(String s) {
        IDirectoryObjectCollectionWithReferencesPage aaa = graphClient.groups(s).members().buildRequest().get();
        System.out.println(111);
    }

    private void changeGroupVisiable(String groupId) {
        Group group = new Group();
        group.visibility = "public";
        graphClient.groups(groupId)
                .buildRequest()
                .patch(group);
    }

    private void groupSetting(String groupId) {
        IGroupSettingCollectionPage groupSettings = graphClient.groups(groupId).settings()
                .buildRequest()
                .get();
        System.out.println("111");
    }

    private void getGroupOwner(String groupId) {
        IDirectoryObjectCollectionWithReferencesPage aaa = graphClient.groups(groupId).owners().buildRequest().get();
        System.out.println(111);
    }

    private Group updateGroup(String groupId) {
        Group group = new Group();
        group.autoSubscribeNewMembers = false;
        return graphClient.groups(groupId).buildRequest().patch(group);
    }

    private void listSite() {
        ISiteCollectionPage sites = graphClient.sites().buildRequest().get();
        System.out.println(111);
    }

    public SharePointFolder createFolder2(String site, String folderName, String conflictBehavior) {
        Assert.hasText(site, "组id不能为空");
        Assert.hasText(folderName, "文件夹名称不能为空");
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
        DriveItem driveItem = new DriveItem();
        driveItem.name = getRealFolderName(folderName);
        Folder folder = new Folder();
        driveItem.folder = folder;
        if (StringUtils.isBlank(conflictBehavior)) {
            conflictBehavior = CONFLICT_BEHAVIOR_RENAME;
        }
        driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive(conflictBehavior));
        try {
            driveItem = graphClient.sites(site).drive().root().children().buildRequest().post(driveItem);
        } catch (GraphServiceException ex) {
            if (ex.getResponseCode() == 409) {
                driveItem = graphClient.sites(site).drive().root().itemWithPath(spaceEncode(driveItem.name)).buildRequest().get();
            } else {
                throw ex;
            }
        }
        SharePointFolder sharePointFolder = new SharePointFolder();
        sharePointFolder.setFolderId(driveItem.id);
        sharePointFolder.setPath(driveItem.name);
        sharePointFolder.setGroupId(site);
        sharePointFolder.setFolderName(driveItem.name);
        return sharePointFolder;
    }

    private void queryFoldById(String groupId, String folderId) {
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new QueryOption("$filter", "id eq '" + folderId + "'"));
        IDriveItemCollectionPage aaa = graphClient.groups(groupId).drive().root().children().buildRequest(requestOptions).get();
        System.out.println(111);
    }


    private void createSite() {
        Site site = new Site();
        site.displayName = "first-site";
        site.name = "first-site";
        graphClient.sites().buildRequest().post(site);
    }

    public Permission queryPermissionOfItemById(String groupId, String itemPath, String permissionsId) {
        try {
            Permission permission = graphClient.groups(groupId).drive().root().itemWithPath(spaceEncode(itemPath)).permissions(permissionsId)
                    .buildRequest()
                    .get();
            return permission;
        } catch (GraphServiceException exception) {
            if (exception.getResponseCode() == 404) {
                return null;
            }
            throw exception;
        }
    }

    public String sharesItemByWeb(String groupId, String itemId, String recipientEmail, boolean requireSignIn, boolean sendInvitation, PermissionEnum permission) throws IOException {
        Assert.hasText(recipientEmail, "接收邮件不能为空");
        LinkedList<DriveRecipient> recipientsList = new LinkedList<>();
        DriveRecipient recipients = new DriveRecipient();
        recipients.email = recipientEmail;
        recipientsList.add(recipients);
        String message = "Here's the file that we're collaborating on.";
        LinkedList<String> rolesList = new LinkedList<String>();
        if (permission == PermissionEnum.READ) {
            rolesList.add("read");
        } else if (permission == PermissionEnum.WRITE) {
            rolesList.add("write");
        }

        String url = String.format(" https://graph.microsoft.com/v1.0/groups/%s/drive/items/root:/%s:/invite", groupId,itemId);
        OkHttpClient client = HttpClients.createDefault(authProvider);
        Gson gson = new Gson();
        HashMap<String, Object> hashMap = new HashMap<>(8);
        hashMap.put("requireSignIn", requireSignIn);
        hashMap.put("sendInvitation",sendInvitation);
        hashMap.put("roles",rolesList);
        hashMap.put("recipients",recipientsList);
        hashMap.put("message",message);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(hashMap));
        Request request = new Request.Builder().addHeader("Content-type", "application/json")
                .addHeader("Content-length", String.valueOf(requestBody.contentLength())).url(url).post(requestBody).build();
        Response response1 = client.newCall(request).execute();
        if (!response1.isSuccessful()) {
            System.out.println(response1.body().string());
        }
        return "";
    }

    void getUserDrive(String userId) throws IOException {
        IDriveItemCollectionPage items = graphClient.users(userId).drive().items().buildRequest().get();
        System.out.println(222);
    }

    public User querySharePointUserById(String id) {
        String fields = "id,displayName,mail,userPrincipalName";
        LinkedList<Option> requestOptions = new LinkedList<Option>();
        requestOptions.add(new QueryOption("$filter", "(id eq '" + id + "')"));
        IUserCollectionPage userPage = graphClient.users().buildRequest(requestOptions).top(1).select(fields).get();
        User user = null;
        if (userPage.getCurrentPage().size() > 0) {
            user = userPage.getCurrentPage().get(0);
        }
        return user;
    }

    public static String spaceEncode(String str) {
        if (str == null || str == "") {
            return str;
        }
        return str.replaceAll(" ", "%20").replaceAll(" ","%20");
    }
}
