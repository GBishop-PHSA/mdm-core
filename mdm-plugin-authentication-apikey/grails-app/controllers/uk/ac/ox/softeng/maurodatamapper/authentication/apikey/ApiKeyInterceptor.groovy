package uk.ac.ox.softeng.maurodatamapper.authentication.apikey

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor

class ApiKeyInterceptor implements MdmInterceptor {

    ApiKeyAuthenticatingService apiKeyAuthenticatingService

    public static final String API_KEY_HEADER = ApiKeyAuthenticationInterceptor.API_KEY_HEADER

    boolean before() {
        if (actionName == 'test') {
            String apiKeyHeader = request.getHeader(API_KEY_HEADER)
            if (!apiKeyHeader) {
                forbidden('No API key found.')
                return false
            }
            if (!apiKeyAuthenticatingService.authenticateAndObtainUserUsingApiKey(apiKeyHeader)) {
                forbidden('API key is invalid.')
                return false
            }
            true
        }
    }
}
