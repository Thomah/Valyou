CREATE OR REPLACE FUNCTION public.create_donation(_account_id bigint, _campaign_id bigint, _budget_id bigint, _amount real)
 RETURNS boolean
 LANGUAGE plpgsql
AS $function$
	declare
		_account_amount FLOAT4;
		_user_id INT8;
		_donation_id INT8;
	begin

		-- Verify that contributor has enough amount
		select amount into _account_amount from accounts where id = _account_id;
		IF _account_amount < _amount THEN
			RAISE EXCEPTION 'Not enough amount on account %', _account_id
				USING HINT = 'Please check your budget';
			return false;
		END IF;

		select nextval('hibernate_sequence') into _donation_id;
		select a.owner_id into _user_id from accounts a where a.id = _account_id;
		insert into donations (id, amount, budget_id, contributor_id, campaign_id, account_id)
			values(_donation_id, _amount, _budget_id, _user_id, _campaign_id, _account_id);
		update accounts set amount = (amount - _amount) where id = _account_id;
		update campaigns set total_donations = total_donations  + _amount where id = _campaign_id;
		update budgets set total_donations = total_donations + _amount where id = _budget_id;

		return true;
	END;
$function$
;

CREATE OR REPLACE FUNCTION public.delete_donation(_donation_id bigint)
 RETURNS boolean
 LANGUAGE plpgsql
AS $function$
	declare
		_account_id INT8;
		_campaign_id INT8;
		_budget_id INT8;
		_amount FLOAT4;
	BEGIN
		select account_id, campaign_id, budget_id, amount
			into _account_id, _campaign_id, _budget_id, _amount
			from donations where id = _donation_id;
		update budgets set total_donations = total_donations - _amount where id = _budget_id;
		update campaigns set total_donations = total_donations  - _amount where id = _campaign_id;
		update accounts set amount = (amount + _amount) where id = _account_id;
		delete from donations where id = _donation_id;
		return true;
	END;
$function$
;

CREATE OR REPLACE PROCEDURE public.update_campaigns_total_donations()
	LANGUAGE plpgsql
AS $procedure$
	declare
		campaign_cursor	CURSOR FOR select d.campaign_id , sum(d.amount) as total_donations from donations d group by d.campaign_id;
		campaignId	campaigns.id%TYPE;
		campaignTotalDonations	campaigns.total_donations%TYPE;
	BEGIN
		OPEN campaign_cursor;
		FETCH campaign_cursor INTO campaignId, campaignTotalDonations;
		RAISE NOTICE 'update campaigns set total_donations = % where id = %', campaignTotalDonations, campaignId;
		update campaigns set total_donations = campaignTotalDonations where id = campaignId;
		close campaign_cursor;
	END;
$procedure$
;
;
