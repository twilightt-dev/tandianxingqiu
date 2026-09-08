package com.dianping.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void createsTypedSuccessAndErrorEnvelopes() {
        Result<String> success = Result.success("token");

        assertThat(success.getCode()).isEqualTo(1);
        assertThat(success.getMsg()).isNull();
        assertThat(success.getData()).isEqualTo("token");

        Result<Void> error = Result.error("失败");

        assertThat(error.getCode()).isZero();
        assertThat(error.getMsg()).isEqualTo("失败");
        assertThat(error.getData()).isNull();
    }
}
