package com.oh.oh.mapper;

import com.oh.oh.entity.OhosRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OhosRecordMapper {

    @Insert({
        "<script>",
        "SET IDENTITY_INSERT ohos_record ON;",
        "INSERT INTO ohos_record(id, ohos_temperature, ohos_humidity) VALUES(#{id}, #{ohosTemperature}, #{ohosHumidity});",
        "SET IDENTITY_INSERT ohos_record OFF;",
        "</script>"
    })
    int insertRecord(OhosRecord record);

    @Select("SELECT TOP 10 * FROM ohos_record")
    List<OhosRecord> findAll();

    @Select("SELECT TOP 10 ohos_temperature FROM ohos_record WHERE ohos_temperature IS NOT NULL ORDER BY id DESC")
    List<Double> findAllTemperatures();

    @Select("SELECT TOP 10 ohos_humidity FROM ohos_record WHERE ohos_humidity IS NOT NULL ORDER BY id DESC")
    List<Double> findAllHumidities();

}
