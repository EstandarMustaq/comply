# Complyz domain language

Complyz supports financial institutions with separate human identity, operational tenant and compliance domains. Use these terms consistently in code, APIs and product language.

## Identity and organization

**Account**:
A person's identity and profile used to sign in. An Account may hold memberships in multiple Organizations; the Account itself grants no access to their compliance records.
_Avoid_: User, customer account

**Organization**:
The client institution represented in the product and the tenant boundary for its policies, memberships and compliance work. A person's access to an Organization is explicit and never flows from a commercial reference.
_Avoid_: Corporação; tenant ID as a user-facing label. Use “Organização” in customer-facing Portuguese and keep `Organization` as the stable domain identifier.

**Organization membership**:
An explicit relationship between an Account and one Organization, carrying the role and status that determine the Account's access in that Organization.
_Avoid_: CommercialClient membership

**Organization unit**:
A representation of an Organization, such as its headquarters, branch or agency, to which an OCOS designation may be assigned.
_Avoid_: tenant subdivision

## Commercial reference

**CommercialClient**:
A segregated commercial namespace reference used exclusively for commercial control in the platform back office. It does not parent, group or manage Organizations and has no role in authorization or Organization entitlement decisions.
_Avoid_: commercial tenant, commercial user, commercial role

## Compliance

**KYC Client**:
A person or business whose identity or activity is subject to an Organization's customer due diligence. A KYC Client is distinct from an Account and an Organization.
_Avoid_: unqualified “client” when it could mean a CommercialClient

**OCOS**:
The Organization-designated officer responsible for deciding and submitting suspicious transaction reports and for the confidentiality of their contents.
_Avoid_: maker, checker

**OCOS coordinator**:
The Organization's active OCOS designation that coordinates Organization-wide suspicious transaction reporting and follow-up.
_Avoid_: second approver
