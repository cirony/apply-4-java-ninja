/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.quickstarts.kitchensink.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jboss.as.quickstarts.kitchensink.data.MemberRepository;
import org.jboss.as.quickstarts.kitchensink.exception.UniqueConstraintException;
import org.jboss.as.quickstarts.kitchensink.model.Member;
import org.jboss.as.quickstarts.kitchensink.service.MemberRegistration;

/**
 * JAX-RS Example
 * <p/>
 * This class produces a RESTful service to read/write the contents of the members table.
 */
@Path("/members")
@RequestScoped
public class MemberResourceRESTService {

    @Inject
    private Logger log;

    @Inject
    private Validator validator;

    @Inject
    private MemberRepository repository;

    @Inject
    MemberRegistration registration;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Member> listAllMembers() {
        return repository.findAllOrderedByName();
    }

    @GET
    @Path("/{id:[0-9][0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Member lookupMemberById(@PathParam("id") long id) {
        Member member = repository.findById(id);
        if (member == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return member;
    }

    /**
     * Creates a new member from the values provided. Performs validation, and will return a JAX-RS response with either 200 ok,
     * or with a map of fields, and related errors.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMember(Member member) {

        Response.ResponseBuilder builder = null;

        try {
            // Validates member using bean validation
            validateMember(member);

            registration.register(member);

            // Create an "ok" response
            builder = Response.ok();
        } catch (ConstraintViolationException ce) {
            // Handle bean validation issues
            builder = createViolationResponse(ce.getConstraintViolations());
        } catch (UniqueConstraintException e) {
            // Handle the unique constrain violation
            builder = createUniqueConstraintExceptionResponse(e.getConflictiveParameters());
        } catch (Exception e) {
            // Handle generic exceptions
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
        }

        return builder.build();
    }
    
    /**
     * <p>
     * Validates the given Member variable and throws validation exceptions based on the type of error. If the error is standard
     * bean validation errors then it will throw a ConstraintValidationException with the set of the constraints violated.
     * </p>
     * <p>
     * If the error is caused because an existing member with the same email or name is registered it throws a UniqueConstraintException so that it can be interpreted separately.
     * </p>
     *
     * @param member Member to be validated
     * @throws ConstraintViolationException If Bean Validation errors exist
     * @throws UniqueConstraintException If member with the same email or name already exists
     */
    private void validateMember(Member member) throws ConstraintViolationException, ValidationException, UniqueConstraintException {
        // Create a bean validator and check for issues.
        Set<ConstraintViolation<Member>> violations = validator.validate(member);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(new HashSet<ConstraintViolation<?>>(violations));
        }

        // Check the uniqueness of the email address and name
        Set<String> conflictiveParam = checkUniqueContraints(member);

        
        if(!conflictiveParam.isEmpty()){
        	throw new UniqueConstraintException("Unique constraint violations were found", conflictiveParam);
        }
    }

    /**
     * Creates a JAX-RS "Bad Request" response including a map of all violation fields, and their message. This can then be used
     * by clients to show violations.
     *
     * @param violations A set of violations that needs to be reported
     * @return JAX-RS response containing all violations
     */
    private Response.ResponseBuilder createViolationResponse(Set<ConstraintViolation<?>> violations) {
        log.fine("Validation completed. violations found: " + violations.size());

        Map<String, String> responseObj = new HashMap<>();

        for (ConstraintViolation<?> violation : violations) {
            responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
    }
    
    /**
     * Creates a JAX-RS "Conflict" response including the warnings to be displayed for each field with "Unique Constraint" validation. 
     *
     * @param conflictiveParameters The name of the parameters where a UniqueConstraintException were found.
     * @return JAX-RS response containing all the warnings to be displayed by the conflictive field
     */
    private ResponseBuilder createUniqueConstraintExceptionResponse(Set<String> conflictiveParameters){

        // Handle the unique constrain violation
    	Map<String, String> responseObj = new HashMap<>();
        
    	Iterator<String> it = conflictiveParameters.iterator();
    	
    	while(it.hasNext()){
    		String param = it.next();
    		responseObj.put(param.toLowerCase(), param + " taken");
    	}

        return Response.status(Response.Status.CONFLICT).entity(responseObj);
    }

    /**
     * Checks if a member with the same email address or name is already registered. This is the only way to easily capture the
     * "@UniqueConstraint(columnNames = {"email", "name"})" constraint from the Member class.
     *
     * @param member The member which data should be checked
     * @return conflictiveParam The names of the parameters with conflicts
     */
    private Set<String> checkUniqueContraints(Member member){
    	 Set<String> conflictiveParam = new HashSet<String>();
    	 
        if (parameterAlreadyExists("email", member.getEmail())) {
        	conflictiveParam.add("Email");
        }
        if (parameterAlreadyExists("name", member.getName())) {
        	conflictiveParam.add("Name");
        }
        
        return conflictiveParam;
    }
    
    /**
     * Check if the given parameter exists in the database
     * @param parameter The parameter name to evaluate
     * @param value The parameter value to compare
 	 * @return true If founds a member with the same parameter value registered
 	 */
    private boolean parameterAlreadyExists(String parameter, String value) {
        Member member = null;
        try {
            member = repository.findByParam(parameter, value);
        } catch (NoResultException e) {
            // ignore
        }
        return member != null;
    }
    
    
}
