package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")//鐢ㄦ埛琛紝瀛樺偍绯荤粺涓殑鐢ㄦ埛淇℃伅锛屽寘鎷敤鎴峰悕銆佸瘑鐮併€佽仈绯绘柟寮忋€佸鏍′俊鎭瓑
@Schema(description = "鐢ㄦ埛瀹炰綋绫伙紝鍖呭惈鐢ㄦ埛鐨勮缁嗕俊鎭拰鍏宠仈鐨勭畝鍘嗐€佽亴浣嶆帹鑽愮瓑")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String realName;

    private String avatarUrl;

    private String school;

    private String major;

    private String grade;

    private String role;

    private String accountType;

    private Integer phoneVerified;

    private Integer emailVerified;

    private Integer enabled;

    private LocalDateTime lastLoginTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}

