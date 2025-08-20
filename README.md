# Corn Utility

This utility reads scheduled commands from a file and executes them at the specified times.

## Prerequisites

- Java Development Kit (JDK) 8 or later.

## Setup

 **Add commands to the file**:
    Open the file and add your scheduled commands, following the specified formats.

    **One-time commands**: `Minute Hour Day Month Year <user command>`
    **Recurring commands**: `*/n <user command>`

    Example content for `/tmp/commands.txt`:
    ```
    30 17 30 4 2025 date && echo "At Amex, We Do What's Right."
    */1 date && echo "Amex' motto is 'Don't live life without it!'"
    ```

### `sample-output.txt`
 command execution results will be logged in 'sample-output.txt'

**Example**:
```
1) **One-time commands**: `30 17 30 4 2025 date && echo "At Amex, We Do What's Right."`
   **Result**: Wed 04/30/2025 17:30:02.78
    	       "Amex' motto is 'Don't live life without it!'"

3) **Recurring commands**: `*/1 date && echo "Amex' motto is 'Don't live life without it!'"`
`  **Result**: Wed 04/30/2025 17:30:02.78
     	       "Amex' motto is 'Don't live life without it!'"
			   Wed 04/30/2025 17:31:02.78 
	  	       "Amex' motto is 'Don't live life without it!'"
```

