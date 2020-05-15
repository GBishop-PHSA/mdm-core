package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

class ApiProperty implements CreatorAware {

    UUID id
    String key
    String value
    String lastUpdatedBy

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        lastUpdatedBy email: true
    }

    static mapping = {
        value type: 'text'
    }

    @Override
    String getDomainType() {
        ApiProperty.simpleName
    }

    def beforeValidate() {
        if (!lastUpdatedBy) lastUpdatedBy = createdBy
    }

    def beforeInsert() {
        if (!lastUpdatedBy) lastUpdatedBy = createdBy
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        ApiProperty that = (ApiProperty) o

        if (id == that.id) return true

        if (key != that.key) return false
        if (value != that.value) return false

        return true
    }

    @Override
    int hashCode() {
        int result
        result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}
