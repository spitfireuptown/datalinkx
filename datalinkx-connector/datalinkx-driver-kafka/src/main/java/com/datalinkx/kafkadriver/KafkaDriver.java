package com.datalinkx.kafkadriver;

import com.datalinkx.common.result.DatalinkXJobDetail;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.common.utils.TelnetUtil;
import com.datalinkx.driver.dsdriver.IStreamDriver;
import com.datalinkx.driver.dsdriver.base.AbstractDriver;
import com.datalinkx.driver.dsdriver.base.reader.ReaderInfo;
import com.datalinkx.driver.dsdriver.base.writer.WriterInfo;
import com.datalinkx.driver.dsdriver.setupinfo.KafkaSetupInfo;

public class KafkaDriver extends AbstractDriver<KafkaSetupInfo, KafkaReader, KafkaWriter> implements IStreamDriver {
    private final KafkaSetupInfo kafkaSetupInfo;
    private final String connectId;
    public KafkaDriver(String connectId) {
        this.kafkaSetupInfo = JsonUtils.toObject(ConnectIdUtils.decodeConnectId(connectId) , KafkaSetupInfo.class);
        this.connectId = connectId;
    }


    @Override
    public Object connect(boolean check) throws Exception {
        TelnetUtil.telnet(this.kafkaSetupInfo.getServer(), this.kafkaSetupInfo.getPort());
        return null;
    }


    @Override
    public Object getReaderInfo(DatalinkXJobDetail.Reader reader) {
        ReaderInfo<KafkaReader> readerInfo = new ReaderInfo<>();
        readerInfo.setName("kafkacustomreader");

        readerInfo.setParameter(KafkaReader.builder()
                .topic(reader.getTableName())
                .codec("text")
                .blankIgnore(false)
                .consumerSettings(CommonSetting.builder().bootstrapServers(kafkaSetupInfo.getServer() + ":" + kafkaSetupInfo.getPort()).build())
                        .column(reader.getColumns())
                .build());

        return readerInfo;
    }

    @Override
    public Object getWriterInfo(DatalinkXJobDetail.Writer writer) {
        WriterInfo<KafkaWriter> writerInfo = new WriterInfo<>();
        writerInfo.setName("kafkacustomwriter");

        writerInfo.setParameter(KafkaWriter.builder().topic(writer.getTableName())
                .timezone(kafkaSetupInfo.getTimezone())
                .producerSettings(CommonSetting.builder().bootstrapServers(kafkaSetupInfo.getServer() + ":" + kafkaSetupInfo.getPort()).build())
                .tableFields(writer.getToColumns())
                .build());
        return writerInfo;
    }
}
