package com.regnosys.rosetta.common.serialisation.csv;


import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0.master-SNAPSHOT
 */
@RosettaDataType(value="User", builder=User.UserBuilderImpl.class, version="0.0.0.master-SNAPSHOT")
@RuneDataType(value="User", model="demo", builder=User.UserBuilderImpl.class, version="0.0.0.master-SNAPSHOT")
public interface User extends RosettaModelObject {

    UserMeta metaData = new UserMeta();

    /*********************** Getter Methods  ***********************/
    String getUsername();
    String getIdentifier();
    String getFirstName();
    String getLastName();

    /*********************** Build Methods  ***********************/
    User build();

    User.UserBuilder toBuilder();

    static User.UserBuilder builder() {
        return new User.UserBuilderImpl();
    }

    /*********************** Utility Methods  ***********************/
    @Override
    default RosettaMetaData<? extends User> metaData() {
        return metaData;
    }

    @Override
    @RuneAttribute("@type")
    default Class<? extends User> getType() {
        return User.class;
    }

    @Override
    default void process(RosettaPath path, Processor processor) {
        processor.processBasic(path.newSubPath("username"), String.class, getUsername(), this);
        processor.processBasic(path.newSubPath("identifier"), String.class, getIdentifier(), this);
        processor.processBasic(path.newSubPath("firstName"), String.class, getFirstName(), this);
        processor.processBasic(path.newSubPath("lastName"), String.class, getLastName(), this);
    }


    /*********************** Builder Interface  ***********************/
    interface UserBuilder extends User, RosettaModelObjectBuilder {
        User.UserBuilder setUsername(String username);
        User.UserBuilder setIdentifier(String identifier);
        User.UserBuilder setFirstName(String firstName);
        User.UserBuilder setLastName(String lastName);

        @Override
        default void process(RosettaPath path, BuilderProcessor processor) {
            processor.processBasic(path.newSubPath("username"), String.class, getUsername(), this);
            processor.processBasic(path.newSubPath("identifier"), String.class, getIdentifier(), this);
            processor.processBasic(path.newSubPath("firstName"), String.class, getFirstName(), this);
            processor.processBasic(path.newSubPath("lastName"), String.class, getLastName(), this);
        }


        User.UserBuilder prune();
    }

    /*********************** Immutable Implementation of User  ***********************/
    class UserImpl implements User {
        private final String username;
        private final String identifier;
        private final String firstName;
        private final String lastName;

        protected UserImpl(User.UserBuilder builder) {
            this.username = builder.getUsername();
            this.identifier = builder.getIdentifier();
            this.firstName = builder.getFirstName();
            this.lastName = builder.getLastName();
        }

        @Override
        @RosettaAttribute(value="username", isRequired=true)
        @RuneAttribute(value="username", isRequired=true)
        public String getUsername() {
            return username;
        }

        @Override
        @RosettaAttribute(value="identifier", isRequired=true)
        @RuneAttribute(value="identifier", isRequired=true)
        public String getIdentifier() {
            return identifier;
        }

        @Override
        @RosettaAttribute(value="firstName", isRequired=true)
        @RuneAttribute(value="firstName", isRequired=true)
        public String getFirstName() {
            return firstName;
        }

        @Override
        @RosettaAttribute(value="lastName", isRequired=true)
        @RuneAttribute(value="lastName", isRequired=true)
        public String getLastName() {
            return lastName;
        }

        @Override
        public User build() {
            return this;
        }

        @Override
        public User.UserBuilder toBuilder() {
            User.UserBuilder builder = builder();
            setBuilderFields(builder);
            return builder;
        }

        protected void setBuilderFields(User.UserBuilder builder) {
            ofNullable(getUsername()).ifPresent(builder::setUsername);
            ofNullable(getIdentifier()).ifPresent(builder::setIdentifier);
            ofNullable(getFirstName()).ifPresent(builder::setFirstName);
            ofNullable(getLastName()).ifPresent(builder::setLastName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;

            User _that = getType().cast(o);

            if (!Objects.equals(username, _that.getUsername())) return false;
            if (!Objects.equals(identifier, _that.getIdentifier())) return false;
            if (!Objects.equals(firstName, _that.getFirstName())) return false;
            if (!Objects.equals(lastName, _that.getLastName())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int _result = 0;
            _result = 31 * _result + (username != null ? username.hashCode() : 0);
            _result = 31 * _result + (identifier != null ? identifier.hashCode() : 0);
            _result = 31 * _result + (firstName != null ? firstName.hashCode() : 0);
            _result = 31 * _result + (lastName != null ? lastName.hashCode() : 0);
            return _result;
        }

        @Override
        public String toString() {
            return "User {" +
                    "username=" + this.username + ", " +
                    "identifier=" + this.identifier + ", " +
                    "firstName=" + this.firstName + ", " +
                    "lastName=" + this.lastName +
                    '}';
        }
    }

    /*********************** Builder Implementation of User  ***********************/
    class UserBuilderImpl implements User.UserBuilder {

        protected String username;
        protected String identifier;
        protected String firstName;
        protected String lastName;

        @Override
        @RosettaAttribute(value="username", isRequired=true)
        @RuneAttribute(value="username", isRequired=true)
        public String getUsername() {
            return username;
        }

        @Override
        @RosettaAttribute(value="identifier", isRequired=true)
        @RuneAttribute(value="identifier", isRequired=true)
        public String getIdentifier() {
            return identifier;
        }

        @Override
        @RosettaAttribute(value="firstName", isRequired=true)
        @RuneAttribute(value="firstName", isRequired=true)
        public String getFirstName() {
            return firstName;
        }

        @Override
        @RosettaAttribute(value="lastName", isRequired=true)
        @RuneAttribute(value="lastName", isRequired=true)
        public String getLastName() {
            return lastName;
        }

        @RosettaAttribute(value="username", isRequired=true)
        @RuneAttribute(value="username", isRequired=true)
        @Override
        public User.UserBuilder setUsername(String _username) {
            this.username = _username == null ? null : _username;
            return this;
        }

        @RosettaAttribute(value="identifier", isRequired=true)
        @RuneAttribute(value="identifier", isRequired=true)
        @Override
        public User.UserBuilder setIdentifier(String _identifier) {
            this.identifier = _identifier == null ? null : _identifier;
            return this;
        }

        @RosettaAttribute(value="firstName", isRequired=true)
        @RuneAttribute(value="firstName", isRequired=true)
        @Override
        public User.UserBuilder setFirstName(String _firstName) {
            this.firstName = _firstName == null ? null : _firstName;
            return this;
        }

        @RosettaAttribute(value="lastName", isRequired=true)
        @RuneAttribute(value="lastName", isRequired=true)
        @Override
        public User.UserBuilder setLastName(String _lastName) {
            this.lastName = _lastName == null ? null : _lastName;
            return this;
        }

        @Override
        public User build() {
            return new User.UserImpl(this);
        }

        @Override
        public User.UserBuilder toBuilder() {
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public User.UserBuilder prune() {
            return this;
        }

        @Override
        public boolean hasData() {
            if (getUsername()!=null) return true;
            if (getIdentifier()!=null) return true;
            if (getFirstName()!=null) return true;
            if (getLastName()!=null) return true;
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public User.UserBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
            User.UserBuilder o = (User.UserBuilder) other;


            merger.mergeBasic(getUsername(), o.getUsername(), this::setUsername);
            merger.mergeBasic(getIdentifier(), o.getIdentifier(), this::setIdentifier);
            merger.mergeBasic(getFirstName(), o.getFirstName(), this::setFirstName);
            merger.mergeBasic(getLastName(), o.getLastName(), this::setLastName);
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;

            User _that = getType().cast(o);

            if (!Objects.equals(username, _that.getUsername())) return false;
            if (!Objects.equals(identifier, _that.getIdentifier())) return false;
            if (!Objects.equals(firstName, _that.getFirstName())) return false;
            if (!Objects.equals(lastName, _that.getLastName())) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int _result = 0;
            _result = 31 * _result + (username != null ? username.hashCode() : 0);
            _result = 31 * _result + (identifier != null ? identifier.hashCode() : 0);
            _result = 31 * _result + (firstName != null ? firstName.hashCode() : 0);
            _result = 31 * _result + (lastName != null ? lastName.hashCode() : 0);
            return _result;
        }

        @Override
        public String toString() {
            return "UserBuilder {" +
                    "username=" + this.username + ", " +
                    "identifier=" + this.identifier + ", " +
                    "firstName=" + this.firstName + ", " +
                    "lastName=" + this.lastName +
                    '}';
        }
    }
}
