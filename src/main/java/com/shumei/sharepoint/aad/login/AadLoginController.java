package com.shumei.sharepoint.aad.login;

import com.shumei.sharepoint.util.UserSharePointCache;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

/**
 * @author xushuai
 * @discription aad登录回调地址
 */
@Controller
@RequestMapping("/deal")
@ResponseBody
public class AadLoginController {

    @RequestMapping("/index_cn.html")
    public void index() {
        System.out.println("indexa");
    }

    @RequestMapping("/upload")
    public String upload(HttpServletRequest request) throws IOException {
        MultipartFile file = ((StandardMultipartHttpServletRequest) request).getFile("file");
        String email = request.getParameter("email");
        File newFile = new File(file.getOriginalFilename());
        InputStream ins = file.getInputStream();
        try {
            OutputStream os = new FileOutputStream(newFile);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        UserSharePointCache.getUserSharePoint(email).uploadFile("7a3a753f-5448-4661-91c3-e80878a2cd1f", newFile, "rename", "", false);
        return "success";
    }
}
