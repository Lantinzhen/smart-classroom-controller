package com.oh.oh.mapper;

import com.oh.oh.entity.OhosUserInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OhosUserInfoMapper {

    @Insert({
        "<script>",
        "SET IDENTITY_INSERT ohos_user_info ON;",
        "INSERT INTO ohos_user_info(id, count, password) VALUES(#{id}, #{count}, #{password});",
        "SET IDENTITY_INSERT ohos_user_info OFF;",
        "</script>"
    })
    int insertUserInfo(OhosUserInfo userInfo);

    @Select("SELECT * FROM ohos_user_info WHERE count = #{count}")
    OhosUserInfo findByCount(String count);

    @Select("SELECT * FROM ohos_user_info WHERE count = #{count} AND password = #{password}")
    OhosUserInfo findByCountAndPassword(@Param("count") String count, @Param("password") String password);

    @Select("SELECT * FROM ohos_user_info")
    List<OhosUserInfo> findAll();

    @Update("UPDATE ohos_user_info SET password = #{password} WHERE count = #{count}")
    int updatePassword(@Param("count") String count, @Param("password") String password);
}