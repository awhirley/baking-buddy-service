package com.bakingbuddy.api.errors

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ApiErrorCode {
    @SerialName("validation_error")
    VALIDATION_ERROR,

    @SerialName("not_found")
    NOT_FOUND,

    @SerialName("conflict")
    CONFLICT,

    @SerialName("bad_request")
    BAD_REQUEST,

    @SerialName("unprocessable_entity")
    UNPROCESSABLE_ENTITY,

    @SerialName("data_integrity_error")
    DATA_INTEGRITY_ERROR,

    @SerialName("internal_error")
    INTERNAL_ERROR,
}
